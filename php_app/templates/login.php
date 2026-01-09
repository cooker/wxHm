<?php include __DIR__.'/header.php'; ?>
<div style="max-width:480px;margin:0 auto">
<h2>登录</h2>
<form method="post">
    <label>用户名<input name="username" placeholder="用户名或邮箱"></label>
    <label>密码<input type="password" name="password" placeholder="密码"></label>
    <div style="margin-top:8px"><button type="submit">登录</button></div>
    <p style="margin-top:10px" class="muted"><a href="?action=register">没有账号？立即注册</a></p>
    </form>
</div>
<?php include __DIR__.'/footer.php'; ?>