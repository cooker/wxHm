<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>微信活码系统 - PHP</title>
<style>
    :root{--wx-green:#09bb07;--wx-dark:#067a04;--muted:#6b7280;--bg:#f7fdf7;--card-bg:#ffffff}
    body{font-family:Inter,ui-sans-serif,system-ui,Segoe UI,Roboto,"Helvetica Neue",Arial; background:var(--bg); margin:0;color:#0f172a}
    header{background:linear-gradient(90deg,var(--wx-green),#39d353);color:#fff;padding:18px}
    header h1{margin:0;font-size:20px}
    nav{margin-top:8px}
    nav a{color:rgba(255,255,255,0.95);text-decoration:none;margin-right:12px;font-weight:600}
    main{max-width:1100px;margin:24px auto;padding:20px;background:transparent}
    h2{color:var(--wx-dark);margin-top:0}
    form label{display:block;margin:8px 0;color:#111}
    input[type=text],input[type=password],textarea,select{width:100%;padding:10px;border-radius:8px;border:1px solid #e6eef0}
    button{background:var(--wx-green);color:#fff;border:none;padding:10px 16px;border-radius:8px;cursor:pointer}
    .muted{color:var(--muted)}

    /* Cards and layout */
    .card{background:var(--card-bg);border-radius:12px;padding:18px;border:1px solid #eef7ee;box-shadow:0 6px 18px rgba(15,23,42,0.04);}
    .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}
    .stat{font-size:20px;color:var(--wx-green);font-weight:700}
    .muted-small{color:var(--muted);font-size:13px}
    .btn{background:var(--wx-green);color:#fff;border:none;padding:8px 12px;border-radius:8px;cursor:pointer}
    .btn-ghost{background:transparent;border:1px solid #e6eef0;color:var(--wx-dark);padding:8px 12px;border-radius:8px}

    /* Table styles */
    table{width:100%;border-collapse:collapse}
    thead tr{border-bottom:1px solid #eef7ee}
    tbody tr{border-bottom:1px solid #f4f8f4}
    td,th{padding:10px 8px}
    th{color:var(--muted);font-weight:600}

    /* form small */
    .form-inline{display:flex;gap:8px;align-items:center}
    .chip{display:inline-block;padding:6px 10px;border-radius:999px;background:#f0fff0;color:var(--wx-dark);font-weight:600}
</style>
</head>
<body>
<header>
    <h1>微信活码系统</h1>
    <nav><a href="?action=home">首页</a> <a href="?action=dashboard">用户中心</a> <a href="?action=admin">后台</a></nav>
</header>
<main>
