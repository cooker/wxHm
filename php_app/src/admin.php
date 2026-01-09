<?php
require_once __DIR__.'/auth.php';
require_once __DIR__.'/plan.php';

function admin_dashboard() {
    require_admin();
    $plans = list_plans();
    include __DIR__.'/../templates/admin.php';
}

function get_setting($k) {
    global $pdo;
    $stmt = $pdo->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
    $stmt->execute([':k'=>$k]);
    $r = $stmt->fetch();
    return $r ? $r['v'] : null;
}

function set_setting($k, $v) {
    global $pdo;
    $stmt = $pdo->prepare('REPLACE INTO settings (`k`, `v`) VALUES (:k, :v)');
    return $stmt->execute([':k'=>$k, ':v'=>$v]);
}

function admin_settings() {
    require_admin();
    $payment = get_setting('payment');
    $payment = $payment ? json_decode($payment, true) : [];
    // fallback to config.php defaults when DB not set
    if (empty($payment)) {
        $cfg = require __DIR__ . '/config.php';
        $payment = $cfg['payment'] ?? [];
    }
    include __DIR__.'/../templates/admin_settings.php';
}

function admin_save_settings() {
    require_admin();
    $payment = [
        'yipay' => [
            'merchant_id' => $_POST['yipay_merchant_id'] ?? '',
            'merchant_key' => $_POST['yipay_merchant_key'] ?? '',
            'pay_url' => $_POST['yipay_pay_url'] ?? '',
            'notify_url' => $_POST['yipay_notify_url'] ?? '',
        ],
        'alipay' => [
            'app_id' => $_POST['alipay_app_id'] ?? '',
            'private_key' => $_POST['alipay_private_key'] ?? '',
            'alipay_public_key' => $_POST['alipay_public_key'] ?? '',
            'notify_url' => $_POST['alipay_notify_url'] ?? '',
        ]
    ];
        // affiliate rates
        $affiliate = [
            'rates' => [
                floatval($_POST['aff_rate_1'] ?? 0.10),
                floatval($_POST['aff_rate_2'] ?? 0.05),
                floatval($_POST['aff_rate_3'] ?? 0.02),
            ]
        ];
        set_setting('affiliate', json_encode($affiliate));
    // wechat config
    $payment['wechat'] = [
        'appid' => $_POST['wechat_appid'] ?? '',
        'mch_id' => $_POST['wechat_mch_id'] ?? '',
        'key' => $_POST['wechat_key'] ?? '',
        'notify_url' => $_POST['wechat_notify_url'] ?? '',
    ];
    set_setting('payment', json_encode($payment));
    // 同步回写到 src/config.php 的 notify_url 和 site.base_url
    $configPath = __DIR__ . '/config.php';
    if (is_writable($configPath)) {
        $cfg = require $configPath;
        $cfg['payment']['yipay']['notify_url'] = $payment['yipay']['notify_url'];
        $cfg['payment']['yipay']['pay_url'] = $payment['yipay']['pay_url'];
        $cfg['payment']['yipay']['merchant_id'] = $payment['yipay']['merchant_id'];
        $cfg['payment']['yipay']['merchant_key'] = $payment['yipay']['merchant_key'];
        $cfg['payment']['alipay']['notify_url'] = $payment['alipay']['notify_url'];
        $cfg['payment']['alipay']['app_id'] = $payment['alipay']['app_id'];
        $cfg['payment']['alipay']['private_key'] = $payment['alipay']['private_key'];
        $cfg['payment']['alipay']['alipay_public_key'] = $payment['alipay']['alipay_public_key'];
        $cfg['payment']['wechat'] = $cfg['payment']['wechat'] ?? [];
        $cfg['payment']['wechat']['appid'] = $payment['wechat']['appid'] ?? '';
        $cfg['payment']['wechat']['mch_id'] = $payment['wechat']['mch_id'] ?? '';
        $cfg['payment']['wechat']['key'] = $payment['wechat']['key'] ?? '';
        $cfg['payment']['wechat']['notify_url'] = $payment['wechat']['notify_url'] ?? '';
        $cfg['site']['base_url'] = $cfg['site']['base_url'] ?? '';
        $content = "<?php\nreturn ".var_export($cfg, true).";\n";
        @file_put_contents($configPath, $content);
    }
    header('Location: ?action=admin_settings');
}

function admin_create_plan_from_request() {
    require_admin();
    $name = $_POST['name'] ?? '';
    $type = $_POST['type'] ?? 'monthly';
    $price = $_POST['price'] ?? 0;
    $count = $_POST['count'] ?? null;
    $period = $_POST['period_months'] ?? null;
    create_plan($name, $type, $price, $count, $period);
    header('Location: ?action=admin');
}

function admin_delete_plan_from_request() {
    require_admin();
    $id = intval($_GET['plan_id'] ?? 0);
    if ($id) delete_plan($id);
    header('Location: ?action=admin');
}

function admin_affiliate() {
    require_admin();
    global $pdo;
    // 支持分页和搜索
    $page = max(1, intval($_GET['page'] ?? 1));
    $per = 50;
    $offset = ($page - 1) * $per;
    $q = trim($_GET['q'] ?? '');
    $params = [];
    $where = '';
    if ($q !== '') {
        $where = 'WHERE u.username LIKE :q OR o.trade_no LIKE :q';
        $params[':q'] = "%$q%";
    }
    $stmt = $pdo->prepare('SELECT a.*, u.username FROM affiliate_earnings a LEFT JOIN users u ON a.user_id = u.id LEFT JOIN orders o ON a.order_id = o.id ' . $where . ' ORDER BY a.created_at DESC LIMIT :off, :lim');
    $stmt->bindValue(':off', $offset, PDO::PARAM_INT);
    $stmt->bindValue(':lim', $per, PDO::PARAM_INT);
    foreach ($params as $k=>$v) $stmt->bindValue($k, $v);
    $stmt->execute();
    $earnings = $stmt->fetchAll();
    // refs
    $stmt = $pdo->prepare('SELECT r.*, u.username as referrer_name FROM referrals r LEFT JOIN users u ON r.referrer_id = u.id ' . ($q ? 'WHERE u.username LIKE :q' : '') . ' ORDER BY r.created_at DESC LIMIT :off, :lim');
    $stmt->bindValue(':off', $offset, PDO::PARAM_INT);
    $stmt->bindValue(':lim', $per, PDO::PARAM_INT);
    if ($q) $stmt->bindValue(':q', "%$q%");
    $stmt->execute();
    $refs = $stmt->fetchAll();
    // total counts for pagination (simple)
    $stmt = $pdo->query('SELECT COUNT(*) as c FROM affiliate_earnings');
    $totalE = intval($stmt->fetchColumn());
    $totalPages = max(1, ceil($totalE / $per));
    include __DIR__.'/../templates/admin_affiliate.php';
}

function admin_withdrawals() {
    require_admin();
    global $pdo;
    // 支持筛选与分页
    $page = max(1, intval($_GET['page'] ?? 1));
    $per = 50;
    $offset = ($page - 1) * $per;
    $status = $_GET['status'] ?? '';
    $where = '';
    $params = [];
    if (in_array($status, ['pending','processed','rejected'])) {
        $where = 'WHERE w.status = :status';
        $params[':status'] = $status;
    }
    $sql = 'SELECT w.*, u.username FROM withdrawals w LEFT JOIN users u ON w.user_id = u.id ' . $where . ' ORDER BY w.created_at DESC LIMIT :off, :lim';
    $stmt = $pdo->prepare($sql);
    foreach ($params as $k=>$v) $stmt->bindValue($k, $v);
    $stmt->bindValue(':off', $offset, PDO::PARAM_INT);
    $stmt->bindValue(':lim', $per, PDO::PARAM_INT);
    $stmt->execute();
    $withdrawals = $stmt->fetchAll();

    $countSql = 'SELECT COUNT(*) FROM withdrawals w ' . $where;
    $stmt = $pdo->prepare($countSql);
    foreach ($params as $k=>$v) $stmt->bindValue($k, $v);
    $stmt->execute();
    $total = intval($stmt->fetchColumn());
    $totalPages = max(1, ceil($total / $per));
    include __DIR__.'/../templates/admin_withdrawals.php';
}

function admin_process_withdrawal() {
    require_admin();
    global $pdo;
    $id = intval($_GET['id'] ?? 0);
    $do = $_GET['do'] ?? '';
    if (!$id) { header('Location: ?action=admin_withdrawals'); exit; }
    if ($do === 'approve') {
        $stmt = $pdo->prepare('UPDATE withdrawals SET status = "processed" WHERE id = :id');
        $stmt->execute([':id'=>$id]);
    } elseif ($do === 'reject') {
        $stmt = $pdo->prepare('UPDATE withdrawals SET status = "rejected" WHERE id = :id');
        $stmt->execute([':id'=>$id]);
    }
    header('Location: ?action=admin_withdrawals');
    exit;
}

function admin_batch_process_withdrawals() {
    require_admin();
    global $pdo;
    $ids = $_POST['ids'] ?? [];
    $action = $_POST['action_type'] ?? '';
    if (!is_array($ids) || empty($ids)) { header('Location: ?action=admin_withdrawals'); exit; }
    $valid = ['approve'=>'processed','reject'=>'rejected'];
    if (!isset($valid[$action])) { header('Location: ?action=admin_withdrawals'); exit; }
    $status = $valid[$action];
    $in = implode(',', array_map('intval', $ids));
    $sql = "UPDATE withdrawals SET status = :status WHERE id IN ($in)";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':status'=>$status]);
    header('Location: ?action=admin_withdrawals');
    exit;
}

function admin_export_withdrawals() {
    require_admin();
    global $pdo;
    $ids = $_POST['ids'] ?? null;
    header('Content-Type: text/csv');
    header('Content-Disposition: attachment; filename="withdrawals_export_' . date('Ymd_His') . '.csv"');
    $out = fopen('php://output', 'w');
    fputcsv($out, ['id','user_id','username','amount','status','meta','created_at']);
    if (is_array($ids) && !empty($ids)) {
        $in = implode(',', array_map('intval', $ids));
        $sql = "SELECT w.*, u.username FROM withdrawals w LEFT JOIN users u ON w.user_id = u.id WHERE w.id IN ($in) ORDER BY w.created_at DESC";
        $stmt = $pdo->query($sql);
        foreach ($stmt->fetchAll() as $r) {
            fputcsv($out, [$r['id'],$r['user_id'],$r['username'],$r['amount'],$r['status'],$r['meta'],$r['created_at']]);
        }
    } else {
        // export all
        $stmt = $pdo->query('SELECT w.*, u.username FROM withdrawals w LEFT JOIN users u ON w.user_id = u.id ORDER BY w.created_at DESC');
        foreach ($stmt->fetchAll() as $r) {
            fputcsv($out, [$r['id'],$r['user_id'],$r['username'],$r['amount'],$r['status'],$r['meta'],$r['created_at']]);
        }
    }
    fclose($out);
    exit;
}
