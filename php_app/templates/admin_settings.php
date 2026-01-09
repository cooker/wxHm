<?php include __DIR__.'/header.php'; ?>
<?php $config = require __DIR__ . '/../src/config.php'; ?>
<div style="max-width:820px;margin:0 auto">
<h2>后台设置 - 支付配置</h2>
<form method="post" action="?action=admin_save_settings">
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:18px">
    <div style="padding:12px;border:1px solid #eef7ee;border-radius:8px">
        <h3 style="margin-top:0">易支付（优先）</h3>
        <label>商户号 <input name="yipay_merchant_id" value="<?=htmlspecialchars($payment['yipay']['merchant_id'] ?? '')?>"></label>
        <label>商户密钥 <input name="yipay_merchant_key" value="<?=htmlspecialchars($payment['yipay']['merchant_key'] ?? '')?>"></label>
        <label>支付网关 URL <input name="yipay_pay_url" value="<?=htmlspecialchars($payment['yipay']['pay_url'] ?? '')?>"></label>
        <label>回调地址（notify） <input name="yipay_notify_url" value="<?=htmlspecialchars($payment['yipay']['notify_url'] ?? '')?>"></label>
    </div>
    <div style="padding:12px;border:1px solid #eef7ee;border-radius:8px">
        <h3 style="margin-top:0">支付宝</h3>
        <label>App ID <input name="alipay_app_id" value="<?=htmlspecialchars($payment['alipay']['app_id'] ?? '')?>"></label>
        <label>私钥 <textarea name="alipay_private_key" style="height:120px"><?=htmlspecialchars($payment['alipay']['private_key'] ?? '')?></textarea></label>
        <label>支付宝公钥 <textarea name="alipay_public_key" style="height:120px"><?=htmlspecialchars($payment['alipay']['alipay_public_key'] ?? '')?></textarea></label>
        <label>回调地址（notify） <input name="alipay_notify_url" value="<?=htmlspecialchars($payment['alipay']['notify_url'] ?? '')?>"></label>
    </div>
    <div style="padding:12px;border:1px solid #eef7ee;border-radius:8px;grid-column:1/-1">
        <h3 style="margin-top:0">微信支付</h3>
        <label>AppID <input name="wechat_appid" value="<?=htmlspecialchars($payment['wechat']['appid'] ?? '')?>"></label>
        <label>商户号 (mch_id) <input name="wechat_mch_id" value="<?=htmlspecialchars($payment['wechat']['mch_id'] ?? '')?>"></label>
        <label>API Key <input name="wechat_key" value="<?=htmlspecialchars($payment['wechat']['key'] ?? '')?>"></label>
        <label>回调地址（notify） <input name="wechat_notify_url" value="<?=htmlspecialchars($payment['wechat']['notify_url'] ?? '')?>"></label>
    </div>
    </div>
    <div style="margin-top:12px"><button type="submit">保存设置</button></div>
    <hr>
    <h3>推广联盟设置</h3>
    <?php
    // 读取 affiliate 设置
    $aff = [];
    if (!empty($payment)) {
        // payment 存在时从 settings 表读取 affiliate
        try {
            $db = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $config['db']['host'], $config['db']['port'], $config['db']['dbname']), $config['db']['user'], $config['db']['pass']);
            $stmt = $db->prepare('SELECT v FROM settings WHERE `k` = :k LIMIT 1');
            $stmt->execute([':k'=>'affiliate']);
            $r = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($r) $aff = json_decode($r['v'], true);
        } catch (Exception $e) { $aff = []; }
    }
    $rates = $aff['rates'] ?? [0.10,0.05,0.02];
    ?>
    <label>一级分成比例（小数）<input name="aff_rate_1" value="<?=htmlspecialchars($rates[0])?>"></label>
    <label>二级分成比例（小数）<input name="aff_rate_2" value="<?=htmlspecialchars($rates[1])?>"></label>
    <label>三级分成比例（小数）<input name="aff_rate_3" value="<?=htmlspecialchars($rates[2])?>"></label>
    </form>
</div>
<?php include __DIR__.'/footer.php'; ?>
