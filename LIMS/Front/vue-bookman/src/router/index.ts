import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/UserDashboard.vue'
import PersonalCenter from '../views/PersonalCenter.vue'
import Login from '../views/Login.vue'
import Register from '@/views/Register.vue'
import Borrowing from '@/views/Borrowing.vue'
import BookClass from '@/views/BookClass.vue'
import Top from '@/views/Top.vue'
import ReadingAndSharing from '../views/Suggestion.vue' // 交流与分享页面路由
import KnowledgeBase from '../views/KnowledgeBase.vue' // 知识库页面路由
import FaceLogin from '../views/FaceLogin.vue' // 人脸登录页面路由
import Aiqa from '../views/Aiqa.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/personal-center',
      name: 'PersonalCenter',
      component: PersonalCenter // 添加该路由
    },
    {
      path: '/login',
      name: 'Login',
      component: Login // 根据实际路径调整
    },
    {
      path: '/register',
      name: 'Register',
      component: Register
    },
    {
      path: '/face-login',
      name: 'FaceLogin',
      component: FaceLogin
    },

    {
      path: '/borrow/:bookTitle?', // 使用动态参数 :bookTitle 接收书名
      name: 'Borrowing',
      component: Borrowing,
    },
    {
      path: '/search',
      name: 'SearchList',
      component: () => import('@/views/SearchList.vue')
    },
    {
      path: '/bookclass',
      name: 'Bookclass',
      component: BookClass // 添加 BookList 路由
    },
    {
      path: '/top',
      name: 'Top',
      component: Top
    },
    {
      path: '/knowledge',
      name: 'KnowledgeBase',
      component: KnowledgeBase
    },
    {
      path: '/reading-and-sharing',
      name: 'ReadingAndSharing',
      component: ReadingAndSharing // 交流与分享页面
    },
    {
      path: '/shelve',
      name: 'Shelve',
      component: () => import('@/views/ShelvesControl.vue')
    },
    {
      path:'/suggestionctrl',
      name:'SuggestionCtrl',
      component:()=>import('@/views/SuggestionControl.vue')
    },
    {
      path: '/recordctrl',
      name: 'RecordCtrl',
      component:()=>import('@/views/Adminrecord.vue')
    },
    {
      path: '/adminbook',
      name: 'Adminbook',
      component: ()=>import('@/views/Adminbook.vue')
    },
    {
      path: '/adminuser',
      name: 'Adminuser',
      component: ()=>import('@/views/Adminuser.vue')
    },
    {
      path: '/aiqa',
      name: 'Aiqa',
      component: Aiqa
    }
  ],
})

export default router