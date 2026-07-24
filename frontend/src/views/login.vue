<template>
  <a-row class="login">
    <a-col :span="8" :offset="8" class="login-main">
      <h1 style="text-align: center">minipay支付系统</h1>
      <a-form
          :model="loginForm"
          name="basic"
          autocomplete="off"
      >
        <a-form-item
        label=""
        name="mobile"
        :rules="[{ required: true, message: '请输入手机号!' }]"
        >
          <a-input v-model:value="loginForm.mobile" placeholder="手机号"/>
        </a-form-item>

        <a-form-item
            label=""
            name="code"
            :rules="[{ required: true, message: '请输入验证码!' }]">
          <a-input v-model:value="loginForm.code">
            <template #addonAfter>
              <a @click="sendCode">获取验证码</a>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block @click="login">登录</a-button>
        </a-form-item>
      </a-form>
    </a-col>
  </a-row>
</template>

<script>
import { defineComponent, reactive } from 'vue';
import { useRouter } from 'vue-router';
import axios from "axios";
import {notification} from "ant-design-vue";

export default defineComponent({
  name: "login-view",
  setup() {
    const router = useRouter();

    const loginForm = reactive({
      mobile: '13000000000',
      code: '',
    });

    function login() {
      axios.post("/api/login/login", loginForm).then((response) => {
        let data = response.data;
        if(data.success) {
          localStorage.setItem('token', data.data.token);
          let msg = data.data.newUser ? '新用户注册成功，欢迎使用！' : '登录成功！';
          notification.success({ description: msg });
          router.push('/products');
        } else {
          notification.error({ description: data.message });
        }
      }).catch((error) => {
        let msg = error.response?.data?.message || '登录失败，请稍后重试';
        notification.error({ description: msg });
      });
    };

    function sendCode() {
      loginForm.code = "8888";
      console.log('sendCode', loginForm.mobile);
      notification.success({ description: '发送验证码成功！' });
    }

    return {
      loginForm,
      login,
      sendCode,
    };
  }
})
</script>
