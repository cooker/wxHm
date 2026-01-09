<?php include __DIR__.'/header.php'; ?>
<h2>首次安装 - 填写数据库信息</h2>
<?php if (!empty($error)): ?>
    <div style="color:red"><?=htmlspecialchars($error)?></div>
<?php endif; ?>
<form method="post">
    <label>数据库主机 <input name="db_host" value="<?=htmlspecialchars($values['db_host'] ?? '127.0.0.1')?>"></label><br>
    <label>数据库端口 <input name="db_port" value="<?=htmlspecialchars($values['db_port'] ?? '3306')?>"></label><br>
    <label>数据库名 <input name="db_name" value="<?=htmlspecialchars($values['db_name'] ?? 'wxhm')?>"></label><br>
    <label>数据库用户 <input name="db_user" value="<?=htmlspecialchars($values['db_user'] ?? 'root')?>"></label><br>
    <label>数据库密码 <input name="db_pass" value="<?=htmlspecialchars($values['db_pass'] ?? '')?>"></label><br>
    <label>站点 URL <input name="base_url" value="<?=htmlspecialchars($values['base_url'] ?? '')?>"></label><br>
    <hr>
    <h3>管理员账号</h3>
    <label>管理员用户名 <input name="admin_user" value="<?=htmlspecialchars($values['admin_user'] ?? 'admin')?>"></label><br>
    <label>管理员密码 <input name="admin_pass" value="<?=htmlspecialchars($values['admin_pass'] ?? 'admin')?>"></label><br>
    <button type="submit">安装并初始化</button>
</form>
<?php include __DIR__.'/footer.php'; ?>
