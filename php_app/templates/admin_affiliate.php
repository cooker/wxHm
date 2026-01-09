<?php include __DIR__.'/header.php'; ?>
<div style="max-width:1100px;margin:20px auto">
    <div style="display:flex;justify-content:space-between;align-items:center">
        <div>
            <h2>推广管理</h2>
            <div class="muted-small">推广佣金流水与推广用户概览</div>
        </div>
    </div>

    <div class="card" style="margin-top:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
            <h3 style="margin:0">佣金流水</h3>
            <form method="get" style="display:flex;gap:8px;align-items:center">
                <input type="hidden" name="action" value="admin_affiliate">
                <input name="q" placeholder="搜索用户名或订单号" value="<?=htmlspecialchars($_GET['q'] ?? '')?>" style="padding:8px;border-radius:8px;border:1px solid #eef7ee">
                <button class="btn" type="submit">搜索</button>
            </form>
        </div>
        <table>
            <thead>
                <tr><th>ID</th><th>用户</th><th>订单</th><th>金额</th><th>层级</th><th>时间</th></tr>
            </thead>
            <tbody>
            <?php foreach ($earnings as $e): ?>
                <tr><td><?=$e['id']?></td><td><?=htmlspecialchars($e['username'] ?? '用户#'.$e['user_id'])?></td><td><?=htmlspecialchars($e['order_id'])?></td><td style="color:var(--wx-green)">¥<?=number_format($e['amount'],2)?></td><td><?=$e['level']?></td><td><?=$e['created_at']?></td></tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    </div>
    <div style="margin-top:12px;display:flex;justify-content:center;gap:8px">
        <?php for($p=1;$p<=$totalPages;$p++): ?>
            <a class="btn-ghost" href="?action=admin_affiliate&page=<?=$p?>&q=<?=urlencode($_GET['q'] ?? '')?>"><?=$p?></a>
        <?php endfor; ?>
    </div>

    <div class="card" style="margin-top:16px">
        <h3 style="margin:0 0 12px 0">推广用户统计（最近）</h3>
        <table>
            <thead>
                <tr><th>推广者</th><th>被推广用户ID</th><th>层级</th><th>时间</th></tr>
            </thead>
            <tbody>
            <?php foreach ($refs as $r): ?>
                <tr><td><?=htmlspecialchars($r['referrer_name'] ?? '用户#'.$r['referrer_id'])?></td><td><?=$r['referred_id']?></td><td><?=$r['level']?></td><td><?=$r['created_at']?></td></tr>
            <?php endforeach; ?>
            </tbody>
        </table>
    </div>

</div>
<?php include __DIR__.'/footer.php'; ?>
