<?php include __DIR__.'/header.php'; ?>
<h2>后台管理</h2>
<h2>后台管理</h2>
<p><a href="?action=home">前台</a> | <a href="?action=admin_settings">支付设置</a> | <a href="?action=admin_affiliate">推广管理</a> | <a href="?action=admin_withdrawals">提现审核</a></p>
<h3>套餐列表</h3>
<ul>
<?php foreach ($plans as $p): ?>
    <li><?=htmlspecialchars($p['name'])?> — <?=htmlspecialchars($p['type'])?> — ¥<?=number_format($p['price'],2)?> <a href="?action=admin_delete_plan&plan_id=<?=$p['id']?>">删除</a></li>
<?php endforeach; ?>
</ul>
<h3>新增套餐</h3>
<form method="post" action="?action=admin_create_plan">
    <label>名称<input name="name"></label><br>
    <label>类型<select name="type"><option value="monthly">按月</option><option value="yearly">按年</option><option value="count">按次数</option></select></label><br>
    <label>价格<input name="price" value="0.00"></label><br>
    <label>次数（按次数套餐）<input name="count"></label><br>
    <label>周期(月)（按月/年套餐可填写月数）<input name="period_months"></label><br>
    <button type="submit">创建</button>
</form>
<?php include __DIR__.'/footer.php'; ?>