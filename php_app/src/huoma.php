<?php
require_once __DIR__ . '/db.php';

function get_huoma($id) {
    global $pdo;
    $stmt = $pdo->prepare('SELECT * FROM huoma WHERE id = :id LIMIT 1');
    $stmt->execute([':id'=>$id]);
    return $stmt->fetch();
}

function handle_huoma_redirect($id) {
    if (!$id) {
        http_response_code(400);
        echo 'Invalid id';
        exit;
    }
    global $pdo;
    $huoma = get_huoma($id);
    if (!$huoma) {
        http_response_code(404);
        echo '活码不存在';
        exit;
    }
    $user_id = $huoma['user_id'];

    // 检查是否需要显示提示页面（用户设置）
    try {
        $stmt = $pdo->prepare('SELECT show_prompt, prompt_content FROM users WHERE id = :id LIMIT 1');
        $stmt->execute([':id'=>$user_id]);
        $us = $stmt->fetch();
        if ($us && intval($us['show_prompt']) === 1) {
            // 渲染提示页面并返回
            $contentHtml = $us['prompt_content'] ? nl2br(htmlspecialchars($us['prompt_content'])) : '即将跳转到目标页面';
            $target = $huoma['target_url'];
            include __DIR__ . '/../templates/huoma_prompt.php';
            exit;
        }
    } catch (Exception $e) {
        // ignore and continue redirect
    }

    // 事务：检查订阅并（必要时）扣减次数，同时增加点击数
    try {
        $pdo->beginTransaction();

        // 优先检查时长型订阅（未过期）
        $stmt = $pdo->prepare('SELECT * FROM subscriptions WHERE user_id = :uid AND expires_at IS NOT NULL AND expires_at > NOW() LIMIT 1');
        $stmt->execute([':uid'=>$user_id]);
        $active_time_sub = $stmt->fetch();

        if ($active_time_sub) {
            // 有有效时长订阅，仅记录点击
            $stmt = $pdo->prepare('UPDATE huoma SET clicks = clicks + 1 WHERE id = :id');
            $stmt->execute([':id'=>$id]);
            $pdo->commit();
            header('Location: ' . $huoma['target_url']);
            exit;
        }

        // 否则检查按次数的订阅
        $stmt = $pdo->prepare('SELECT * FROM subscriptions WHERE user_id = :uid AND remaining_count IS NOT NULL AND remaining_count > 0 LIMIT 1 FOR UPDATE');
        $stmt->execute([':uid'=>$user_id]);
        $count_sub = $stmt->fetch();
        if ($count_sub) {
            // 扣减一次
            $new = intval($count_sub['remaining_count']) - 1;
            if ($new < 0) $new = 0;
            $stmt = $pdo->prepare('UPDATE subscriptions SET remaining_count = :r WHERE id = :id');
            $stmt->execute([':r'=>$new, ':id'=>$count_sub['id']]);
            // 增加点击
            $stmt = $pdo->prepare('UPDATE huoma SET clicks = clicks + 1 WHERE id = :id');
            $stmt->execute([':id'=>$id]);
            $pdo->commit();
            header('Location: ' . $huoma['target_url']);
            exit;
        }

        // 没有有效订阅
        $pdo->commit();
        echo '该账户无有效套餐，无法跳转';
        exit;

    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        http_response_code(500);
        echo '跳转失败: ' . $e->getMessage();
        exit;
    }
}
