<?php include __DIR__.'/header.php'; ?>
<div style="max-width:900px;margin:20px auto">
    <div style="display:flex;justify-content:space-between;align-items:center">
        <div>
            <h2>提现审核</h2>
            <div class="muted-small">管理员可在此查看并处理用户的提现申请</div>
        </div>
    </div>

    <div class="card" style="margin-top:12px">
        <form method="post" action="?action=admin_batch_process_withdrawals">
            <div style="display:flex;gap:8px;align-items:center;margin-bottom:10px">
                <select name="action_type">
                    <option value="approve">批量批准</option>
                    <option value="reject">批量拒绝</option>
                </select>
                <button type="submit" class="btn">执行</button>
            </div>
            <table>
                <thead>
                    <tr><th><input id="chk_all" type="checkbox"></th><th>ID</th><th>用户</th><th>金额</th><th>状态</th><th>时间</th><th>操作</th></tr>
                </thead>
                <tbody>
                <?php foreach ($withdrawals as $w): ?>
                    <tr>
                        <td><input type="checkbox" name="ids[]" value="<?=$w['id']?>"></td>
                        <td><?=$w['id']?></td>
                        <td><?=htmlspecialchars($w['username'] ?? '用户#'.$w['user_id'])?></td>
                        <td style="color:var(--wx-green)">¥<?=number_format($w['amount'],2)?></td>
                        <td><?=$w['status']?></td>
                        <td><?=$w['created_at']?></td>
                        <td>
                            <?php if ($w['status'] === 'pending'): ?>
                                <a class="btn-ghost" href="?action=admin_process_withdrawal&id=<?=$w['id']?>&do=approve">批准</a>
                                <a class="btn-ghost" href="?action=admin_process_withdrawal&id=<?=$w['id']?>&do=reject">拒绝</a>
                            <?php else: ?>
                                —
                            <?php endif; ?>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        </form>

        <form method="post" action="?action=admin_export_withdrawals" style="margin-top:10px">
            <button type="submit" class="btn">导出为 CSV（选中则导出选中）</button>
        </form>
    </div>
    <script>
        document.getElementById('chk_all').addEventListener('change', function(e){
            document.querySelectorAll('input[name="ids[]"]').forEach(function(cb){cb.checked = e.target.checked});
        });
    </script>
</div>
<?php include __DIR__.'/footer.php'; ?>
