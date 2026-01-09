<?php
require_once __DIR__.'/db.php';

function list_plans() {
    global $pdo;
    $stmt = $pdo->query('SELECT * FROM plans ORDER BY id ASC');
    return $stmt->fetchAll();
}

function get_plan($id) {
    global $pdo;
    $stmt = $pdo->prepare('SELECT * FROM plans WHERE id = :id');
    $stmt->execute([':id'=>$id]);
    return $stmt->fetch();
}

function create_plan($name, $type, $price, $count=null, $period_months=null) {
    global $pdo;
    $stmt = $pdo->prepare('INSERT INTO plans (name,type,price,count,period_months) VALUES (:n,:t,:p,:c,:m)');
    return $stmt->execute([':n'=>$name,':t'=>$type,':p'=>$price,':c'=>$count,':m'=>$period_months]);
}

function update_plan($id, $name, $type, $price, $count=null, $period_months=null) {
    global $pdo;
    $stmt = $pdo->prepare('UPDATE plans SET name=:n,type=:t,price=:p,count=:c,period_months=:m WHERE id=:id');
    return $stmt->execute([':n'=>$name,':t'=>$type,':p'=>$price,':c'=>$count,':m'=>$period_months,':id'=>$id]);
}

function delete_plan($id) {
    global $pdo;
    $stmt = $pdo->prepare('DELETE FROM plans WHERE id=:id');
    return $stmt->execute([':id'=>$id]);
}
