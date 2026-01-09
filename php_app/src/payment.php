<?php
require_once __DIR__.'/db.php';
$config = require __DIR__.'/config.php';

function create_order($user_id, $plan_id, $amount, $provider='yipay', $meta=null) {
    global $pdo;
    $stmt = $pdo->prepare('INSERT INTO orders (user_id, plan_id, amount, status, payment_provider, meta) VALUES (:uid,:pid,:amt,"pending",:prov,:meta)');
    $stmt->execute([':uid'=>$user_id,':pid'=>$plan_id,':amt'=>$amount,':prov'=>$provider,':meta'=>$meta]);
    return $pdo->lastInsertId();
}

function mark_order_paid($order_id, $trade_no=null) {
    global $pdo;
    $stmt = $pdo->prepare('UPDATE orders SET status="paid", trade_no = :t WHERE id = :id');
    $stmt->execute([':t'=>$trade_no, ':id'=>$order_id]);
}

function handle_provider_callback($provider, $params) {
    // 简化示例：实际请务必验证签名、金额和商户号
    global $pdo;
    if ($provider === 'yipay' || $provider === 'alipay') {
        // 示例：我们假设回调包含 order_id 和 trade_no
        $order_id = $params['order_id'] ?? null;
        $trade_no = $params['trade_no'] ?? null;
        if (!$order_id) return false;
        $stmt = $pdo->prepare('SELECT * FROM orders WHERE id = :id LIMIT 1');
        $stmt->execute([':id'=>$order_id]);
        $order = $stmt->fetch();
        if (!$order) return false;
        if ($order['status'] === 'paid') return true;
        // 标记支付成功，并根据套餐更新订阅
        mark_order_paid($order_id, $trade_no);
        apply_subscription_after_payment($order);
        // 推广分成
        require_once __DIR__ . '/affiliate.php';
        credit_affiliate_commission($order);
        return true;
    }
    return false;
}

function apply_subscription_after_payment($order) {
    global $pdo;
    // 读取 plan
    $stmt = $pdo->prepare('SELECT * FROM plans WHERE id = :id');
    $stmt->execute([':id'=>$order['plan_id']]);
    $plan = $stmt->fetch();
    if (!$plan) return;
    $user_id = $order['user_id'];
    if ($plan['type'] === 'monthly' || $plan['type'] === 'yearly') {
        $months = $plan['period_months'] ?? ($plan['type'] === 'monthly' ? 1 : 12);
        // 查找是否已有订阅
        $stmt = $pdo->prepare('SELECT * FROM subscriptions WHERE user_id = :uid AND plan_id = :pid LIMIT 1');
        $stmt->execute([':uid'=>$user_id,':pid'=>$plan['id']]);
        $sub = $stmt->fetch();
        $now = new DateTime();
        if ($sub && $sub['expires_at']) {
            $current = new DateTime($sub['expires_at']);
            if ($current > $now) {
                $current->modify('+' . $months . ' months');
                $expires = $current->format('Y-m-d H:i:s');
                $stmt = $pdo->prepare('UPDATE subscriptions SET expires_at = :e WHERE id = :id');
                $stmt->execute([':e'=>$expires, ':id'=>$sub['id']]);
                return;
            }
        }
        $now->modify('+' . $months . ' months');
        $expires = $now->format('Y-m-d H:i:s');
        if ($sub) {
            $stmt = $pdo->prepare('UPDATE subscriptions SET expires_at = :e WHERE id = :id');
            $stmt->execute([':e'=>$expires, ':id'=>$sub['id']]);
        } else {
            $stmt = $pdo->prepare('INSERT INTO subscriptions (user_id, plan_id, expires_at) VALUES (:uid,:pid,:e)');
            $stmt->execute([':uid'=>$user_id,':pid'=>$plan['id'],':e'=>$expires]);
        }
    } elseif ($plan['type'] === 'count') {
        $add = intval($plan['count']);
        $stmt = $pdo->prepare('SELECT * FROM subscriptions WHERE user_id = :uid AND plan_id = :pid LIMIT 1');
        $stmt->execute([':uid'=>$user_id,':pid'=>$plan['id']]);
        $sub = $stmt->fetch();
        if ($sub) {
            $remaining = intval($sub['remaining_count']) + $add;
            $stmt = $pdo->prepare('UPDATE subscriptions SET remaining_count = :r WHERE id = :id');
            $stmt->execute([':r'=>$remaining,':id'=>$sub['id']]);
        } else {
            $stmt = $pdo->prepare('INSERT INTO subscriptions (user_id, plan_id, remaining_count) VALUES (:uid,:pid,:r)');
            $stmt->execute([':uid'=>$user_id,':pid'=>$plan['id'],':r'=>$add]);
        }
    }
}

// 生成易支付支付链接示例（注意：具体参数请根据实际易支付文档调整）
function build_yipay_pay_url($order_id, $amount) {
    $config = require __DIR__.'/config.php';
    $settings = null;
    // 优先读取数据库 settings 表中的配置
    try {
        $db = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $config['db']['host'], $config['db']['port'], $config['db']['dbname']), $config['db']['user'], $config['db']['pass']);
        $stmt = $db->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
        $stmt->execute([':k'=>'payment']);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($r) {
            $settings = json_decode($r['v'], true);
        }
    } catch (Exception $e) {
        $settings = null;
    }
    $yipay = $settings['yipay'] ?? $config['payment']['yipay'];
    $merchant_id = $yipay['merchant_id'] ?? '';
    $merchant_key = $yipay['merchant_key'] ?? '';
    $pay_url = rtrim($yipay['pay_url'] ?? $config['payment']['yipay']['pay_url'], '?');

    // 简单签名：md5(order_id|amount|merchant_key)
    $sign = md5($order_id . '|' . $amount . '|' . $merchant_key);
    $query = http_build_query([
        'merchant_id' => $merchant_id,
        'order_id' => $order_id,
        'amount' => $amount,
        'sign' => $sign,
    ]);
    return $pay_url . '?' . $query;
}

// 验证易支付回调签名（示例）
function verify_yipay_callback($params) {
    // 需要 order_id, amount, sign
    if (empty($params['order_id']) || empty($params['amount']) || empty($params['sign'])) return false;
    $config = require __DIR__.'/config.php';
    try {
        $db = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $config['db']['host'], $config['db']['port'], $config['db']['dbname']), $config['db']['user'], $config['db']['pass']);
        $stmt = $db->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
        $stmt->execute([':k'=>'payment']);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        $settings = $r ? json_decode($r['v'], true) : [];
    } catch (Exception $e) {
        $settings = [];
    }
    $yipay = $settings['yipay'] ?? $config['payment']['yipay'];
    $merchant_key = $yipay['merchant_key'] ?? '';
    $expected = md5($params['order_id'] . '|' . $params['amount'] . '|' . $merchant_key);
    return hash_equals($expected, $params['sign']);
}

// 构建微信支付示例链接（说明性，实际请使用微信统一下单接口）
function build_wechat_pay_url($order_id, $amount) {
    $config = require __DIR__.'/config.php';
    $settings = null;
    try {
        $db = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $config['db']['host'], $config['db']['port'], $config['db']['dbname']), $config['db']['user'], $config['db']['pass']);
        $stmt = $db->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
        $stmt->execute([':k'=>'payment']);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($r) $settings = json_decode($r['v'], true);
    } catch (Exception $e) { $settings = null; }
    $wechat = $settings['wechat'] ?? $config['payment']['wechat'] ?? [];
    $key = $wechat['key'] ?? '';
    $pay_url = $wechat['pay_url'] ?? ''; // optional
    $sign = md5($order_id . '|' . $amount . '|' . $key);
    $query = http_build_query(['appid'=>$wechat['appid'] ?? '', 'mch_id'=>$wechat['mch_id'] ?? '', 'order_id'=>$order_id, 'amount'=>$amount, 'sign'=>$sign]);
    if ($pay_url) return rtrim($pay_url, '?') . '?' . $query;
    // fallback to site base_url + provider
    $base = $config['site']['base_url'] ?? '';
    return rtrim($base, '/') . '/?action=callback&provider=wechat&' . $query;
}

function verify_wechat_callback($params) {
    if (empty($params['order_id']) || empty($params['amount']) || empty($params['sign'])) return false;
    $config = require __DIR__.'/config.php';
    try {
        $db = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $config['db']['host'], $config['db']['port'], $config['db']['dbname']), $config['db']['user'], $config['db']['pass']);
        $stmt = $db->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
        $stmt->execute([':k'=>'payment']);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        $settings = $r ? json_decode($r['v'], true) : [];
    } catch (Exception $e) { $settings = []; }
    $wechat = $settings['wechat'] ?? $config['payment']['wechat'] ?? [];
    $key = $wechat['key'] ?? '';
    $expected = md5($params['order_id'] . '|' . $params['amount'] . '|' . $key);
    return hash_equals($expected, $params['sign']);
}

// 支付宝验签（RSA/RSA2），基于支付宝公钥
function verify_alipay_callback($params) {
    if (empty($params['sign'])) return false;
    $sign = $params['sign'];
    $sign_type = $params['sign_type'] ?? 'RSA2';
    // 排除 sign, sign_type
    $data = $params;
    unset($data['sign']);
    unset($data['sign_type']);
    ksort($data);
    $buf = [];
    foreach ($data as $k => $v) {
        if ($v === '' || $v === null) continue;
        $buf[] = $k . '=' . (is_array($v) ? json_encode($v, JSON_UNESCAPED_UNICODE) : $v);
    }
    $stringToVerify = implode('&', $buf);

    $config = require __DIR__.'/config.php';
    try {
        $db = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $config['db']['host'], $config['db']['port'], $config['db']['dbname']), $config['db']['user'], $config['db']['pass']);
        $stmt = $db->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
        $stmt->execute([':k'=>'payment']);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        $settings = $r ? json_decode($r['v'], true) : [];
    } catch (Exception $e) { $settings = []; }
    $alipay = $settings['alipay'] ?? $config['payment']['alipay'];
    $pub = $alipay['alipay_public_key'] ?? '';
    if (!$pub) return false;

    // 格式化公钥
    $pub = trim($pub);
    if (strpos($pub, 'BEGIN') === false) {
        $pem = "-----BEGIN PUBLIC KEY-----\n" . chunk_split($pub, 64, "\n") . "-----END PUBLIC KEY-----\n";
    } else {
        $pem = $pub;
    }
    $result = false;
    $decoded_sign = base64_decode($sign);
    if ($decoded_sign === false) return false;
    if ($sign_type === 'RSA2') {
        $ok = openssl_verify($stringToVerify, $decoded_sign, $pem, OPENSSL_ALGO_SHA256);
        $result = ($ok === 1);
    } else {
        $ok = openssl_verify($stringToVerify, $decoded_sign, $pem, OPENSSL_ALGO_SHA1);
        $result = ($ok === 1);
    }
    return $result;
}
