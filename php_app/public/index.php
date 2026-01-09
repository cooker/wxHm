<?php
// 简单路由入口
require_once __DIR__.'/../src/auth.php';
require_once __DIR__.'/../src/plan.php';
require_once __DIR__.'/../src/payment.php';
require_once __DIR__.'/../src/admin.php';
require_once __DIR__.'/../src/installer.php';

// 如果未安装（没有 installed.lock），强制进入安装页
if (!file_exists(__DIR__ . '/../installed.lock')) {
    $action = $_GET['action'] ?? 'install';
} else {
    $action = $_GET['action'] ?? 'home';
}

// 处理推广 ref 参数并写入 cookie，保留 30 天
if (!empty($_GET['ref'])) {
    $ref = preg_replace('/[^a-zA-Z0-9_\-]/', '', $_GET['ref']);
    setcookie('ref', $ref, time() + 30*24*3600, '/');
}


switch ($action) {
    case 'home':
        $plans = list_plans();
        include __DIR__.'/../templates/index.php';
        break;
    case 'install':
        if (file_exists(__DIR__ . '/../installed.lock')) {
            header('Location: ?action=home'); exit;
        }
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            handle_install_post();
        }
        show_install_form();
        break;
    case 'register':
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            $u = $_POST['username'] ?? '';
            $p = $_POST['password'] ?? '';
            $newId = ($u && $p) ? register_user($u,$p) : false;
            if ($newId) {
                // 处理推广关系（若存在 ref cookie 或 ref GET）
                $ref = $_COOKIE['ref'] ?? ($_GET['ref'] ?? null);
                if ($ref) {
                    require_once __DIR__.'/../src/affiliate.php';
                    // ref 可以是用户 id 或 promo_code
                    link_referral($ref, intval($newId));
                }
                header('Location: ?action=login'); exit;
            }
            echo 'Register failed';
            exit;
        }
        include __DIR__.'/../templates/register.php';
        break;
    case 'login':
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            $u = $_POST['username'] ?? '';
            $p = $_POST['password'] ?? '';
            if (login_user($u,$p)) {
                header('Location: ?action=dashboard'); exit;
            }
            echo 'Login failed';
            exit;
        }
        include __DIR__.'/../templates/login.php';
        break;
    case 'logout':
        session_destroy();
        header('Location: ?action=home');
        break;
    case 'dashboard':
        require_login();
        $user = current_user();
        include __DIR__.'/../templates/dashboard.php';
        break;
    case 'request_withdrawal':
        require_login();
        $amount = floatval($_POST['amount'] ?? 0);
        $meta = $_POST['meta'] ?? null;
        require_once __DIR__.'/../src/auth.php';
        $user = current_user();
        if (empty($user['withdraw_account']) || empty($user['withdraw_name'])) {
            echo '请先在左侧填写并保存支付宝账号与真实姓名，再提交提现申请。';
            exit;
        }
        require_once __DIR__.'/../src/affiliate.php';
        if ($amount > 0 && request_withdrawal($_SESSION['user_id'], $amount, $meta)) {
            echo '提现申请提交成功，等待后台处理';
        } else {
            echo '提现申请失败';
        }
        break;

    case 'update_withdraw_info':
        require_login();
        $acc = trim($_POST['withdraw_account'] ?? '');
        $name = trim($_POST['withdraw_name'] ?? '');
        require_once __DIR__.'/../src/auth.php';
        if ($acc === '' || $name === '') {
            echo '账号和姓名不能为空'; exit;
        }
        if (update_user_withdraw_info($_SESSION['user_id'], $acc, $name)) {
            echo '提现信息保存成功';
        } else {
            echo '保存失败';
        }
        break;
    case 'update_prompt_settings':
        require_login();
        $show = isset($_POST['show_prompt']) ? 1 : 0;
        $content = $_POST['prompt_content'] ?? '';
        require_once __DIR__.'/../src/auth.php';
        if (update_user_prompt($_SESSION['user_id'], $show, $content)) {
            echo '设置已保存';
        } else {
            echo '保存失败';
        }
        break;
    case 'buy':
        require_login();
        $plan_id = intval($_GET['plan_id'] ?? 0);
        $plan = get_plan($plan_id);
        if (!$plan) { echo 'Plan not found'; exit; }
        $provider = $_GET['provider'] ?? 'yipay';
        $order_id = create_order($_SESSION['user_id'], $plan_id, $plan['price'], $provider);
        // 根据 provider 生成支付链接
        if ($provider === 'yipay') {
            $payurl = build_yipay_pay_url($order_id, $plan['price']);
        } elseif ($provider === 'wechat') {
            $payurl = build_wechat_pay_url($order_id, $plan['price']);
        } elseif ($provider === 'alipay') {
            // 简化：直接跳转到 alipay 网关示例（实际需构造表单/重定向）
            $payurl = build_yipay_pay_url($order_id, $plan['price']);
        } else {
            $payurl = build_yipay_pay_url($order_id, $plan['price']);
        }
        header('Location: ' . $payurl);
        break;
    case 'callback':
        // 支付回调统一入口（provider=yipay|alipay）
        $provider = $_GET['provider'] ?? null;
        $params = array_merge($_POST, $_GET);
        $ok = false;
        if ($provider === 'yipay') {
            if (function_exists('verify_yipay_callback') && verify_yipay_callback($params)) {
                $ok = handle_provider_callback($provider, $params);
            }
        } elseif ($provider === 'alipay') {
            if (function_exists('verify_alipay_callback') && verify_alipay_callback($params)) {
                $ok = handle_provider_callback($provider, $params);
            }
        } elseif ($provider === 'wechat') {
            if (function_exists('verify_wechat_callback') && verify_wechat_callback($params)) {
                $ok = handle_provider_callback($provider, $params);
            }
        } else {
            $ok = handle_provider_callback($provider, $params);
        }
        if ($ok) {
            echo 'success';
        } else {
            echo 'fail';
        }
        break;

    case 'admin_settings':
        admin_settings();
        break;
    case 'admin_save_settings':
        admin_save_settings();
        break;
    case 'admin_affiliate':
        admin_affiliate();
        break;
    case 'admin_withdrawals':
        admin_withdrawals();
        break;
    case 'admin_process_withdrawal':
        admin_process_withdrawal();
        break;
    case 'admin_batch_process_withdrawals':
        admin_batch_process_withdrawals();
        break;
    case 'admin_export_withdrawals':
        admin_export_withdrawals();
        break;

    case 'go':
        // 活码跳转入口：?action=go&id=XXX
        $id = intval($_GET['id'] ?? 0);
        require_once __DIR__.'/../src/huoma.php';
        handle_huoma_redirect($id);
        break;
    case 'admin':
        admin_dashboard();
        break;
    case 'admin_create_plan':
        admin_create_plan_from_request();
        break;
    case 'admin_delete_plan':
        admin_delete_plan_from_request();
        break;
    default:
        http_response_code(404);
        echo 'Not found';
}
