<?php
// 配置（请按实际环境修改）
return [
    'db' => [
        'host' => '74.50.90.62',
        'port' => 3306,
        'dbname' => 'ceshiwxhm',
        'user' => 'ceshiwxhm',
        'pass' => 'ceshiwxhm',
        'charset' => 'utf8mb4',
    ],

    // 支付配置示例（需替换为真实信息）
    'payment' => [
        'yipay' => [
            'merchant_id' => '',
            'merchant_key' => '',
            'pay_url' => '',
            'notify_url' => 'https://pay.6cloud.net/?action=callback&provider=yipay'
        ],
        'alipay' => [
            'app_id' => '',
            'private_key' => '',
            'alipay_public_key' => '',
            'notify_url' => 'https://pay.6cloud.net/?action=callback&provider=alipay'
        ]
    ],

    // 基本站点配置
    'site' => [
        'base_url' => 'https://pay.6cloud.net',
    ]
];
