import Vue from 'vue';
import Router from 'vue-router';
// 연결할 컴포넌트 import
import store from '../vuex/store';
import Index from '../views/Index.vue';
import Signup from '../views/Signup.vue';
import Login from '../views/Login.vue';
import Permission from '../views/Permission.vue';
import EmailCheck from '../views/EmailCheck.vue';
import Detail from '../views/Detail.vue';
import MyPage from '../views/MyPage.vue';

// 필수
Vue.use(Router);

const requireAuth = () => async (to, from, next) => {
  let token = localStorage.getItem('auth-token');
  if (store.state.authToken == null && token) {
    await store.dispatch('GET_MEMBER_INFO', token);
  }
  if (store.getters.getAuthToken) {
    // 위치 정보 동의 여부
    if ('geolocation' in navigator) {
      return next();
    } else {
      return next('/permission');
    }
  } else next('/login');
};

const routes = [
  {
    path: '/', // 경로
    beforeEnter: requireAuth(),
    component: Index, // 이동할 컴포넌트
    children: [
      {
        path: '',
        name: 'Main',
        component: () => import('../views/index/Main.vue'),
      },
      {
        path: 'sns',
        name: 'SNS',
        beforeEnter: requireAuth(),
        component: () => import('../views/index/SNS.vue'),
      },
      {
        path: 'rank',
        name: 'Rank',
        beforeEnter: requireAuth(),
        component: () => import('../views/index/Rank.vue'),
      },
      {
        path: 'likes',
        name: 'Likes',
        beforeEnter: requireAuth(),
        component: () => import('../views/index/Likes.vue'),
      },
      {
        path: 'badge',
        name: 'Badge',
        beforeEnter: requireAuth(),
        component: () => import('../views/index/Badge.vue'),
      },
    ],
  },
  {
    path: '/mypage',
    name: 'MyPage',
    beforeEnter: requireAuth(),
    component: MyPage,
  },
  {
    path: '/signup',
    name: 'Signup',
    component: Signup,
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },
  {
    path: '/permission',
    name: 'Permission',
    component: Permission,
  },
  {
    path: '/detail',
    name: 'Detail',
    beforeEnter: requireAuth(),
    component: Detail,
  },
  {
    path: '/user/mailCheck',
    name: 'EmailCheck',
    component: EmailCheck,
  },
];

const router = new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes,
  duplicateNavigationPolicy: 'ignore',
});

export default router;
