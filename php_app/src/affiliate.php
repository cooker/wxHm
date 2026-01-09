<?php
require_once __DIR__ . '/db.php';

function link_referral($ref, $newUserId) {
    global $pdo;
    // 如果 ref 为数字，视为 user_id，否则尝试按 promo_code 查询
    $referrer = null;
    if (ctype_digit((string)$ref)) {
        $stmt = $pdo->prepare('SELECT id FROM users WHERE id = :id LIMIT 1');
        $stmt->execute([':id'=>intval($ref)]);
        $referrer = $stmt->fetch();
    } else {
        $stmt = $pdo->prepare('SELECT id FROM users WHERE promo_code = :code LIMIT 1');
        $stmt->execute([':code'=>$ref]);
        $referrer = $stmt->fetch();
    }
    if (!$referrer) return false;
    $referrer_id = $referrer['id'];
    // 阻止自推荐
    if ($referrer_id == $newUserId) return false;
    // 插入一级推荐
    $stmt = $pdo->prepare('INSERT INTO referrals (referrer_id, referred_id, level) VALUES (:r,:n,1)');
    $stmt->execute([':r'=>$referrer_id,':n'=>$newUserId]);
    // 尝试查找二级、三级
    // 二级：查找 referrer's referrer
    $stmt = $pdo->prepare('SELECT referrer_id FROM referrals WHERE referred_id = :rid AND level=1 LIMIT 1');
    $stmt->execute([':rid'=>$referrer_id]);
    $second = $stmt->fetch();
    if ($second && $second['referrer_id']) {
        $stmt = $pdo->prepare('INSERT INTO referrals (referrer_id, referred_id, level) VALUES (:r,:n,2)');
        $stmt->execute([':r'=>$second['referrer_id'],':n'=>$newUserId]);
        // 查三级
        $stmt = $pdo->prepare('SELECT referrer_id FROM referrals WHERE referred_id = :rid AND level=1 LIMIT 1');
        $stmt->execute([':rid'=>$second['referrer_id']]);
        $third = $stmt->fetch();
        if ($third && $third['referrer_id']) {
            $stmt = $pdo->prepare('INSERT INTO referrals (referrer_id, referred_id, level) VALUES (:r,:n,3)');
            $stmt->execute([':r'=>$third['referrer_id'],':n'=>$newUserId]);
        }
    }
    return true;
}

function credit_affiliate_commission($order) {
    global $pdo;
    // 从 settings 中读取分成比例（默认示例）
    $stmt = $pdo->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
    $stmt->execute([':k'=>'affiliate']);
    $r = $stmt->fetch();
    $conf = $r ? json_decode($r['v'], true) : null;
    $rates = $conf['rates'] ?? [0.10, 0.05, 0.02]; // level1,2,3

    // 找到订单所属用户的推荐者链
    $user_id = $order['user_id'];
    // 查找所有 referrals where referred_id = user_id
    $stmt = $pdo->prepare('SELECT referrer_id, level FROM referrals WHERE referred_id = :rid ORDER BY level ASC');
    $stmt->execute([':rid'=>$user_id]);
    $rows = $stmt->fetchAll();
    foreach ($rows as $row) {
        $level = intval($row['level']);
        if ($level < 1 || $level > 3) continue;
        $rate = $rates[$level-1] ?? 0;
        if ($rate <= 0) continue;
        $amount = round($order['amount'] * $rate, 2);
        if ($amount <= 0) continue;
        $stmt = $pdo->prepare('INSERT INTO affiliate_earnings (user_id, order_id, amount, level) VALUES (:uid,:oid,:amt,:lvl)');
        $stmt->execute([':uid'=>$row['referrer_id'], ':oid'=>$order['id'], ':amt'=>$amount, ':lvl'=>$level]);
    }
}

function get_affiliate_summary($user_id) {
    global $pdo;
    $stmt = $pdo->prepare('SELECT SUM(amount) as total FROM affiliate_earnings WHERE user_id = :uid');
    $stmt->execute([':uid'=>$user_id]);
    $r = $stmt->fetch();
    $total = $r['total'] ?? 0;
    $stmt = $pdo->prepare('SELECT COUNT(DISTINCT referred_id) as cnt FROM referrals WHERE referrer_id = :uid');
    $stmt->execute([':uid'=>$user_id]);
    $c = $stmt->fetch();
    $count = $c['cnt'] ?? 0;
    return ['total' => $total, 'referrals' => $count];
}

function request_withdrawal($user_id, $amount, $meta=null) {
    global $pdo;
    $stmt = $pdo->prepare('INSERT INTO withdrawals (user_id, amount, meta) VALUES (:uid,:amt,:meta)');
    return $stmt->execute([':uid'=>$user_id,':amt'=>$amount,':meta'=>$meta]);
}
