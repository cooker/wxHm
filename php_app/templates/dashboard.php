<?php include __DIR__.'/header.php'; ?>
<div style="display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:12px">
    <div>
        <h2>用户中心</h2>
        <div class="muted-small">欢迎：<?php echo htmlspecialchars($user['username']); ?></div>
    </div>
    <div>
        <a class="btn-ghost" href="?action=logout">登出</a>
    </div>
</div>
<h3 class="muted-small">我的订阅 / 套餐</h3>
<?php
require_once __DIR__.'/../src/db.php';
$stmt = $pdo->prepare('SELECT s.*, p.name FROM subscriptions s LEFT JOIN plans p ON s.plan_id = p.id WHERE s.user_id = :uid');
$stmt->execute([':uid'=>$user['id']]);
$subs = $stmt->fetchAll();
?>
<div class="grid" style="margin-bottom:18px">
<?php foreach ($subs as $s): ?>
    <div class="card">
        <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
                <div style="font-weight:700;color:var(--wx-dark)"><?=htmlspecialchars($s['name'] ?? '')?></div>
                <div class="muted-small">到期：<?php echo $s['expires_at'] ?? '-'; ?></div>
            </div>
            <div class="chip"><?php echo $s['remaining_count'] ?? '-'; ?> 次</div>
        </div>
    </div>
<?php endforeach; ?>
</div>
<?php include __DIR__.'/footer.php'; ?>

<?php
// Affiliate summary and controls
require_once __DIR__.'/../src/affiliate.php';
require_once __DIR__.'/../src/config.php';
$config = require __DIR__ . '/../src/config.php';
$aid = $user['id'];
$summary = get_affiliate_summary($aid);
?>
<div style="max-width:980px;margin:20px auto;padding:16px;border-radius:8px;border:1px solid #eef7ee;background:#fff">
    <h3>推广中心</h3>
    <div style="display:flex;gap:16px;align-items:center">
        <div style="flex:1">
            <div style="font-size:20px;color:var(--wx-green)">累计佣金：¥<?php echo number_format($summary['total'] ?? 0,2); ?></div>
            <div class="muted">推广用户数：<?php echo intval($summary['referrals'] ?? 0); ?></div>
        </div>
        <div>
            <button onclick="document.getElementById('withdraw-form').scrollIntoView();">申请提现</button>
        </div>
    </div>
    <hr>
    <h4>推广链接</h4>
    <div style="display:flex;gap:8px;align-items:center">
        <?php $promo = $user['promo_code'] ?? 'user'.$user['id']; $link = ($GLOBALS['config']['site']['base_url'] ?? '') . '/?ref=' . urlencode($promo); ?>
        <input style="flex:1;padding:8px;border-radius:6px;border:1px solid #e6eef0" value="<?php echo htmlspecialchars($link); ?>" readonly>
        <button onclick="navigator.clipboard.writeText('<?php echo htmlspecialchars($link); ?>')">复制</button>
    </div>
    <p class="muted">您可分享到朋友圈或群聊，系统支持 3 级分佣（后台可配置比例）。</p>

    <h4 id="withdraw-form">提现申请</h4>
    <div style="display:flex;gap:16px;align-items:flex-start">
        <div style="flex:1">
            <form method="post" action="?action=update_withdraw_info">
                <h4>提现信息（支付宝）</h4>
                <label>支付宝账号<input name="withdraw_account" value="<?=htmlspecialchars($user['withdraw_account'] ?? '')?>" placeholder="支付宝账号/手机号/邮箱"></label>
                <label>真实姓名<input name="withdraw_name" value="<?=htmlspecialchars($user['withdraw_name'] ?? '')?>" placeholder="收款人姓名"></label>
                <div><button type="submit">保存提现信息</button></div>
            </form>
        </div>
        <div style="width:360px">
            <form method="post" action="?action=request_withdrawal">
                <h4>提交提现申请</h4>
                <label>提现金额<input name="amount" placeholder="¥"></label>
                <label>备注（可选）<input name="meta"></label>
                <div><button type="submit">提交提现申请</button></div>
            </form>
        </div>
    </div>
</div>

<div style="max-width:980px;margin:20px auto;padding:16px;border-radius:8px;border:1px solid #eef7ee;background:#fff">
    <h3>跳转提示页设置</h3>
    <form method="post" action="?action=update_prompt_settings">
        <label><input type="checkbox" name="show_prompt" value="1" <?php echo ($user['show_prompt']? 'checked':'') ?>> 启用跳转提示页</label>
        <label>提示内容<textarea name="prompt_content" style="height:120px"><?php echo htmlspecialchars($user['prompt_content'] ?? '欢迎访问，点击继续跳转至目标群聊/页面。'); ?></textarea></label>
        <div><button type="submit">保存设置</button></div>
    </form>
</div>

