#!/usr/bin/env node
/**
 * 使用 refresh_token 刷新巨量引擎 access_token 的脚本
 *
 * 使用方法：
 *   1. 在同目录下准备 oceanengine_config.json（见注释）
 *   2. npm install axios
 *   3. node refresh_oceanengine_token.js
 */

const fs = require('fs');
const path = require('path');
const axios = require('axios');

// ===== 配置区域 =====
const CONFIG_PATH = path.join(__dirname, 'oceanengine_config.json');
const REFRESH_TOKEN_URL = 'https://ad.oceanengine.com/open_api/oauth2/refresh_token/';
// ===================

// 读取配置
function loadConfig() {
  if (!fs.existsSync(CONFIG_PATH)) {
    throw new Error(`找不到配置文件：${CONFIG_PATH}`);
  }
  const txt = fs.readFileSync(CONFIG_PATH, 'utf8');
  return JSON.parse(txt);
}

// 保存配置
function saveConfig(cfg) {
  fs.writeFileSync(CONFIG_PATH, JSON.stringify(cfg, null, 2), 'utf8');
}

// 时间戳转人类可读
function tsToStr(ts) {
  const n = Number(ts);
  if (!n) return String(ts);
  return new Date(n * 1000).toISOString().replace('T', ' ').substring(0, 19);
}

async function refreshToken() {
  const cfg = loadConfig();

  const appId = cfg.app_id;
  const secret = cfg.secret;
  const refreshTokenVal = cfg.refresh_token;

  if (!appId || !secret || !refreshTokenVal) {
    throw new Error('配置文件中 app_id / secret / refresh_token 必须全部填写');
  }

  const payload = {
    app_id: appId,
    secret: secret,
    refresh_token: refreshTokenVal
  };

  console.log('=== 开始刷新 access_token ===');
  console.log('请求 URL:', REFRESH_TOKEN_URL);
  console.log('请求体:', payload);

  let resp;
  try {
    resp = await axios.post(REFRESH_TOKEN_URL, payload, {
      headers: {
        'Content-Type': 'application/json'
      },
      timeout: 15000
    });
  } catch (err) {
    if (err.response) {
      console.error('HTTP 调用失败，状态码:', err.response.status);
      console.error('返回内容:', err.response.data);
    } else {
      console.error('请求异常:', err.message);
    }
    process.exit(1);
  }

  const data = resp.data;
  console.log('接口返回:', JSON.stringify(data, null, 2));

  const code = data.code;
  if (code !== 0) {
    const msg = data.message || data.msg;
    throw new Error(`刷新失败，code=${code}, msg=${msg}`);
  }

  const info = data.data || {};

  const newAccessToken = info.access_token;
  const newRefreshToken = info.refresh_token || refreshTokenVal;
  const expiresIn = Number(info.expires_in || 0); // 秒
  const refreshExpiresIn = Number(info.refresh_token_expires_in || 0); // 秒

  if (!newAccessToken) {
    throw new Error('返回中没有 access_token，无法更新配置');
  }

  const now = Math.floor(Date.now() / 1000);
  const accessExpiresAt = now + expiresIn;
  const refreshExpiresAt = refreshExpiresIn ? now + refreshExpiresIn : cfg.refresh_token_expires_at || 0;

  // 更新配置文件
  cfg.access_token = newAccessToken;
  cfg.refresh_token = newRefreshToken;
  cfg.access_token_expires_at = accessExpiresAt;
  cfg.refresh_token_expires_at = refreshExpiresAt;

  saveConfig(cfg);

  console.log('\n=== 刷新成功 ===');
  console.log('新的 access_token:', newAccessToken);
  console.log('新的 refresh_token:', newRefreshToken);
  console.log('access_token 过期时间:', tsToStr(accessExpiresAt));
  console.log('refresh_token 过期时间:', tsToStr(refreshExpiresAt));
}

// 入口
refreshToken().catch(err => {
  console.error('\n发生错误:', err.message);
  process.exit(1);
});
