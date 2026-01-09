<?php
function show_install_form($error = null, $values = []) {
    include __DIR__ . '/../templates/install.php';
}

function handle_install_post() {
    $host = $_POST['db_host'] ?? '127.0.0.1';
    $port = intval($_POST['db_port'] ?? 3306);
    $dbname = $_POST['db_name'] ?? 'wxhm';
    $user = $_POST['db_user'] ?? 'root';
    $pass = $_POST['db_pass'] ?? '';
    $base_url = $_POST['base_url'] ?? '';
    $admin_user = $_POST['admin_user'] ?? 'admin';
    $admin_pass = $_POST['admin_pass'] ?? 'admin';

    // 尝试连接到 MySQL（不指定数据库），并创建数据库
    try {
        $dsnNoDb = sprintf('mysql:host=%s;port=%d;charset=utf8mb4', $host, $port);
        $pdo = new PDO($dsnNoDb, $user, $pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
        $pdo->exec("CREATE DATABASE IF NOT EXISTS `" . str_replace('`','',$dbname) . "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
    } catch (Exception $e) {
        show_install_form('数据库连接或创建失败：'.$e->getMessage(), $_POST);
        return;
    }

    // 连接到 new database 并导入 schema（逐条执行以兼容多语句）
    try {
        $dsn = sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $host, $port, $dbname);
        $pdo = new PDO($dsn, $user, $pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
        $schema = file_get_contents(__DIR__ . '/../sql/schema.sql');
        // 简单拆分 SQL 语句并逐条执行（跳过空语句）
        $stmts = preg_split('/;\s*\n/', $schema);
        foreach ($stmts as $sql) {
            $sql = trim($sql);
            if ($sql === '') continue;
            $pdo->exec($sql);
        }
    } catch (Exception $e) {
        show_install_form('导入数据库结构失败：'.$e->getMessage(), $_POST);
        return;
    }

    // 写入配置文件
    $configPath = __DIR__ . '/config.php';
    $configContent = "<?php\nreturn [\n    'db' => [\n        'host' => '".addslashes($host)."',\n        'port' => ".intval($port).",\n        'dbname' => '".addslashes($dbname)."',\n        'user' => '".addslashes($user)."',\n        'pass' => '".addslashes($pass)."',\n        'charset' => 'utf8mb4',\n    ],\n    'payment' => [\n        'yipay' => [\n            'merchant_id' => '',\n            'merchant_key' => '',\n            'pay_url' => '',\n            'notify_url' => '',\n        ],\n        'alipay' => [\n            'app_id' => '',\n            'private_key' => '',\n            'alipay_public_key' => '',\n            'notify_url' => '',\n        ]\n    ],\n    'site' => [\n        'base_url' => '".addslashes($base_url)."',\n    ]\n];\n";

    if (!is_writable(dirname($configPath))) {
        show_install_form('无法写入配置文件，请确保目录可写：'.dirname($configPath), $_POST);
        return;
    }
    file_put_contents($configPath, $configContent);

    // 创建安装锁文件
    file_put_contents(__DIR__ . '/../installed.lock', date('c'));

    // 创建管理员账号
    try {
        $hash = password_hash($admin_pass, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare('INSERT INTO users (username, password_hash, is_admin, promo_code) VALUES (:u,:p,1,:code)');
        $promo = bin2hex(random_bytes(4));
        $stmt->execute([':u'=>$admin_user, ':p'=>$hash, ':code'=>$promo]);
    } catch (Exception $e) {
        // ignore admin creation failure but warn
    }
    // 安装完成，跳转到首页
    header('Location: ?action=home');
    exit;
}
