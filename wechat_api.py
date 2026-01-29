"""
微信公众号API操作工具类
提供获取access_token等基础功能
"""
import requests
import time
from typing import Optional, Dict, Any


class WeChatAPI:
    """微信公众号API工具类"""
    
    def __init__(self, appid: str, secret: str):
        """
        初始化微信公众号API客户端
        
        Args:
            appid: 微信公众号AppID
            secret: 微信公众号AppSecret
        """
        self.appid = appid
        self.secret = secret
        self.base_url = "https://api.weixin.qq.com"
        self._access_token: Optional[str] = None
        self._token_expires_at: float = 0  # token过期时间戳
    
    def get_access_token(self, force_refresh: bool = False) -> Optional[str]:
        """
        获取access_token（带缓存机制）
        
        Args:
            force_refresh: 是否强制刷新token
            
        Returns:
            access_token字符串，失败返回None
        """
        # 如果token未过期且不强制刷新，直接返回缓存的token
        if not force_refresh and self._access_token and time.time() < self._token_expires_at:
            return self._access_token
        
        try:
            url = f"{self.base_url}/cgi-bin/token"
            params = {
                "grant_type": "client_credential",
                "appid": self.appid,
                "secret": self.secret
            }
            
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()
            
            if "access_token" in data:
                self._access_token = data["access_token"]
                # 设置过期时间，提前5分钟刷新（微信token有效期7200秒）
                expires_in = data.get("expires_in", 7200)
                self._token_expires_at = time.time() + expires_in - 300
                return self._access_token
            else:
                error_msg = data.get("errmsg", "未知错误")
                error_code = data.get("errcode", -1)
                print(f"获取access_token失败: {error_code} - {error_msg}")
                return None
                
        except requests.exceptions.RequestException as e:
            print(f"请求access_token时发生网络错误: {str(e)}")
            return None
        except Exception as e:
            print(f"获取access_token时发生未知错误: {str(e)}")
            return None
    
    def _make_api_request(self, endpoint: str, method: str = "GET", 
                          params: Optional[Dict] = None, 
                          json_data: Optional[Dict] = None) -> Optional[Dict[str, Any]]:
        """
        发送API请求的通用方法
        
        Args:
            endpoint: API端点（不包含base_url）
            method: HTTP方法（GET/POST）
            params: URL参数
            json_data: POST请求的JSON数据
            
        Returns:
            API响应的JSON数据，失败返回None
        """
        token = self.get_access_token()
        if not token:
            return None
        
        url = f"{self.base_url}{endpoint}"
        if params is None:
            params = {}
        params["access_token"] = token
        
        try:
            if method.upper() == "GET":
                response = requests.get(url, params=params, timeout=10)
            else:
                response = requests.post(url, params=params, json=json_data, timeout=10)
            
            response.raise_for_status()
            return response.json()
            
        except requests.exceptions.RequestException as e:
            print(f"API请求失败: {str(e)}")
            return None
    
    def is_token_valid(self) -> bool:
        """
        检查当前token是否有效
        
        Returns:
            token有效返回True，否则返回False
        """
        return self._access_token is not None and time.time() < self._token_expires_at
    
    def send_template_message(self, touser: str, template_id: str, 
                             data: Dict[str, Dict[str, str]],
                             url: Optional[str] = None,
                             miniprogram: Optional[Dict[str, str]] = None) -> Optional[Dict[str, Any]]:
        """
        发送模板消息
        
        Args:
            touser: 接收用户的openid
            template_id: 模板消息ID
            data: 模板数据，格式如: {"group": {"value": "111"}, "action": {"value": "111"}}
            url: 可选，模板跳转链接
            miniprogram: 可选，跳转小程序信息，格式如: {"appid": "xxx", "pagepath": "xxx"}
            
        Returns:
            API响应的JSON数据，成功时包含msgid，失败返回None
            
        示例:
            result = wechat.send_template_message(
                touser="own8-3MtxTZc9oX4zQurMp_ijbg8",
                template_id="5luakFpKCJpVCp3DuP8NUQ4AqRqRuChw94uZZXthopw",
                data={
                    "group": {"value": "111"},
                    "action": {"value": "111"},
                    "server": {"value": "111"},
                    "user": {"value": "111"},
                    "time": {"value": "111"}
                }
            )
        """
        payload = {
            "touser": touser,
            "template_id": template_id,
            "data": data
        }
        
        # 可选参数
        if url:
            payload["url"] = url
        if miniprogram:
            payload["miniprogram"] = miniprogram
        
        result = self._make_api_request(
            endpoint="/cgi-bin/message/template/send",
            method="POST",
            json_data=payload
        )
        
        if result:
            errcode = result.get("errcode", 0)
            if errcode == 0:
                return result
            else:
                errmsg = result.get("errmsg", "未知错误")
                print(f"发送模板消息失败: {errcode} - {errmsg}")
                return None
        
        return None


# 使用示例
if __name__ == "__main__":
    # 示例：使用工具类获取token
    wechat = WeChatAPI(
        appid="wx5a2929a3a7dae867",
        secret="60836bf415aa0348a0ffb0f9423d6831"
    )
    
    token = wechat.get_access_token()
    if token:
        print(f"成功获取access_token: {token[:20]}...")
    else:
        print("获取access_token失败")
    
    # 示例：发送模板消息
    result = wechat.send_template_message(
        touser="own8-3MtxTZc9oX4zQurMp_ijbg8",
        template_id="5luakFpKCJpVCp3DuP8NUQ4AqRqRuChw94uZZXthopw",
        data={
            "group": {"value": "111"},
            "action": {"value": "111"},
            "server": {"value": "111"},
            "user": {"value": "111"},
            "time": {"value": "111"}
        }
    )
    
    if result:
        print(f"模板消息发送成功，消息ID: {result.get('msgid')}")
    else:
        print("模板消息发送失败")
