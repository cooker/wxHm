<?php include __DIR__.'/header.php'; ?>
<div class="card" style="max-width:720px;margin:30px auto;text-align:center">
    <h2 style="color:var(--wx-dark)">提示</h2>
    <div style="margin:12px 0;padding:16px;border-radius:10px;background:linear-gradient(180deg,#f8fff8,#ffffff);box-shadow:inset 0 1px 0 rgba(255,255,255,0.6)"> 
        <div style="font-size:15px;color:#0f172a;line-height:1.6"><?php echo $contentHtml; ?></div>
    </div>
    <div style="margin-top:12px">
        <a href="<?php echo htmlspecialchars($target); ?>"><button class="btn">继续访问</button></a>
    </div>
    <div class="muted-small" style="margin-top:12px">提示页可在“用户中心 → 跳转提示页设置”中编辑。</div>
</div>
<?php include __DIR__.'/footer.php'; ?>