<?php include __DIR__.'/header.php'; ?>
<div style="display:flex;gap:20px;align-items:flex-start">
    <section style="flex:1">
        <h2>可购买套餐</h2>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px">
        <?php foreach ($plans as $p): ?>
            <div style="border:1px solid #eef7ee;padding:12px;border-radius:8px">
                <strong style="display:block;font-size:16px;color:var(--wx-dark)"><?=htmlspecialchars($p['name'])?></strong>
                <div class="muted">类型：<?=htmlspecialchars($p['type'])?></div>
                <div style="margin:8px 0;font-size:18px;color:var(--wx-green)">¥<?=number_format($p['price'],2)?></div>
                <form method="get" style="margin-top:8px;display:flex;gap:8px;align-items:center">
                    <input type="hidden" name="action" value="buy">
                    <input type="hidden" name="plan_id" value="<?= $p['id'] ?>">
                    <select name="provider" style="padding:8px;border-radius:6px;border:1px solid #e6eef0">
                        <option value="yipay">易支付</option>
                        <option value="wechat">微信</option>
                        <option value="alipay">支付宝</option>
                    </select>
                    <button type="submit">购买</button>
                </form>
            </div>
        <?php endforeach; ?>
        </div>
    </section>
    <aside style="width:280px">
        <div style="background:linear-gradient(180deg,#fff,#f8fff8);padding:12px;border-radius:8px;border:1px solid #eef6ee">
            <h3 style="margin-top:0">说明</h3>
            <p class="muted">本系统支持按月/按年/按次数付费，后台可配置易支付和支付宝回调地址。</p>
        </div>
    </aside>
</div>
<?php include __DIR__.'/footer.php'; ?>