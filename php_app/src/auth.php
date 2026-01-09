<?php
session_start();
require_once __DIR__.'/db.php';

function register_user($username, $password) {
    global $pdo;
    $hash = password_hash($password, PASSWORD_DEFAULT);
    $stmt = $pdo->prepare('INSERT INTO users (username, password_hash, promo_code) VALUES (:u, :p, :code)');
    $code = bin2hex(random_bytes(4));
    $ok = $stmt->execute([':u'=>$username, ':p'=>$hash, ':code'=>$code]);
    if ($ok) return $pdo->lastInsertId();
    return false;
}

function login_user($username, $password) {
    global $pdo;
    $stmt = $pdo->prepare('SELECT * FROM users WHERE username = :u LIMIT 1');
    $stmt->execute([':u'=>$username]);
    $user = $stmt->fetch();
    if ($user && password_verify($password, $user['password_hash'])) {
        $_SESSION['user_id'] = $user['id'];
        $_SESSION['is_admin'] = $user['is_admin'];
        return true;
    }
    return false;
}

function current_user() {
    global $pdo;
    if (empty($_SESSION['user_id'])) return null;
    $stmt = $pdo->prepare('SELECT id, username, is_admin, created_at, show_prompt, prompt_content, promo_code, withdraw_account, withdraw_name FROM users WHERE id = :id');
    $stmt->execute([':id'=>$_SESSION['user_id']]);
    return $stmt->fetch();
}

function require_login() {
    if (empty($_SESSION['user_id'])) {
        header('Location: ?action=login');
        exit;
    }
}

function require_admin() {
    if (empty($_SESSION['user_id']) || empty($_SESSION['is_admin'])) {
        echo 'Access denied';
        exit;
    }
}

function update_user_prompt($user_id, $show, $content) {
    global $pdo;
    $stmt = $pdo->prepare('UPDATE users SET show_prompt = :s, prompt_content = :c WHERE id = :id');
    return $stmt->execute([':s'=>$show ? 1 : 0, ':c'=>$content, ':id'=>$user_id]);
}

function update_user_withdraw_info($user_id, $account, $name) {
    global $pdo;
    $stmt = $pdo->prepare('UPDATE users SET withdraw_account = :acc, withdraw_name = :name WHERE id = :id');
    return $stmt->execute([':acc'=>$account, ':name'=>$name, ':id'=>$user_id]);
}
