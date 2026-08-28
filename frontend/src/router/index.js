import {createRouter, createWebHistory} from 'vue-router';
import LayoutVue from "@/views/Layout.vue";
import HomePageLayout from "@/views/home/HomePageLayout.vue";
import {useLoadingStore} from "@/stores/loading.js";

// 定义路由关系
const routes = [
    {
        path: '/about',
        component: LayoutVue,
        redirect: '/about/system',
        children: [
            {
                path: '/about/system',
                name: 'AboutSystem',
                component: ()=>import("@/components/AboutSystem.vue"),
                meta: {
                    title: '关于系统',
                    noLayout: true // 标记：不需要显示后台布局（侧边栏、标签栏等）
                }
            },
            {
                path: '/user/avatar',
                name: 'UserAvatar',
                meta:{
                    title:'头像管理'
                },
                component: ()=>import("@/views/user/UserAvatar.vue")
            },
            {
                path: '/user/info',
                name: 'UserInfo',
                meta:{
                    title:'个人信息'
                },
                component: ()=>import("@/views/user/UserInfo.vue")
            },
            {
                path: '/user/resetPassword',
                name: 'UserResetPassword',
                meta:{
                    title:'重置密码'
                },
                component: ()=>import("@/views/user/UserResetPassword.vue")
            },
            {
                path: '/user/manage',
                name: 'UserManage',
                meta:{
                    title:'用户管理'
                },
                component: ()=>import("@/views/user/UserManage.vue")
            },
            {
                path: '/permission/manage',
                name: 'PermissionManage',
                meta:{
                    title:'权限管理'
                },
                component: ()=>import("@/views/user/PermissionManage.vue")
            },
            {
                path: '/roles/manage',
                name: 'RolesManage',
                meta:{
                    title:'角色管理'
                },
                component: ()=>import("@/views/user/RolesManage.vue")
            },
            {
                path: '/roles/rolesGrant',
                name: 'RolesGrant',
                meta:{
                    title:'角色授权'
                },
                component: ()=>import("@/views/user/Grant/RolesGrant.vue")
            },
            {
                path: '/user/userGrant',
                name: 'UsersGrant',
                meta:{
                    title:'用户授权'
                },
                component: ()=>import("@/views/user/Grant/UsersGrant.vue")
            },
            {
                path: '/permission/permissionGrant',
                name: 'PermissionsGrant',
                meta:{
                    title:'权限授权'
                },
                component: ()=>import("@/views/user/Grant/PermissionsGrant.vue")
            },
            {
                path: '/calendar',
                name: 'Calendar',
                meta:{
                    title:'日历'
                },
                component: ()=>import("@/components/Calendar.vue")
            },
        ]
    },
    // ========== 新增：交易后台路由 ==========
    {
        path: '/trade',
        component: LayoutVue,
        redirect: '/trade/category', // 默认跳转到商品分类
        children: [
            // 商品分类
            {
                path: '/trade/category',
                name: 'TradeCategory',
                meta: {
                    title: '商品分类'
                },
                component: ()=>import("@/views/trade/TradeCategory.vue")
            },
            // 订单管理
            {
                path: '/trade/order',
                name: 'TradeOrder',
                meta: {
                    title: '订单管理'
                },
                component: ()=>import("@/views/trade/TradeOrder.vue")
            },
            // 交易商品列表
            {
                path: '/trade/goods/list',
                name: 'TradeGoodsList',
                meta: {
                    title: '交易商品列表'
                },
                component: ()=>import("@/views/trade/TradeGoodsList.vue")
            },
            // 商品发布
            {
                path: '/trade/goods/audit',
                name: 'TradeGoodsAudit',
                meta: {
                    keepAlive: false,
                    title: '商品发布'
                },
                component: ()=>import("@/views/trade/TradeGoodsAudit.vue")
            },
            // 交易统计
            {
                path: '/trade/statistics',
                name: 'TradeStatistics',
                meta: {
                    title: '仪表盘'
                },
                component: ()=>import("@/views/trade/TradeStatistics.vue")
            },
            // 地址管理
            {
                path: '/trade/address',
                name: 'TradeAddress',
                meta: {
                    title: '地址管理'
                },
                component: ()=>import("@/views/trade/TradeAddress.vue")
            },
            // 商品编辑
             {
                 path: '/trade/goods/edit',
                 name: 'TradeGoodsEdit',
                 meta: {
                     keepAlive: false,
                     title: '商品编辑'
                 },
                 component: ()=>import("@/views/trade/TradeGoodsEdits.vue")
             },
            {
                path: '/trade/comment/manage',
                name: 'CommentManage',
                meta:{
                    title:'评论管理'
                },
                component: ()=>import("@/views/trade/CommentManage.vue")
            },
        ]
    },
    // ========== 新增：前台路由 个人中心==========
    {
        path: '/homes/logined',
        name: 'HomesLogined',
        meta:{
            title:'登录'
        },
        component: ()=>import("@/views/home/HomesLogined.vue"),
        redirect: '/homes/mycenter',
        children: [
            //mycenter
            {
                path: '/homes/mycenter',
                name: 'HomesMyCenter',
                meta:{
                    title:'个人中心'
                },
                component: ()=>import("@/views/home/MyCenter.vue")
            },
            //我的商品
            {
                path: '/homes/mygoods',
                name: 'HomesMyGoods',
                meta:{
                    title:'我发布的'
                },
                component: ()=>import("@/views/home/HomesMyGoods.vue")
            },
            {
                path: '/homes/userinfo',
                name: 'HomesUserInfo',
                meta:{
                    title:'个人资料'
                },
                component: ()=>import("@/views/user/UserInfo.vue")
            },
            {
                path: '/homes/resetPassword',
                name: 'HomesResetPassword',
                meta:{
                    title:'重置密码'
                },
                component: ()=>import("@/views/user/UserResetPassword.vue")
            },
            //我的收藏
            {
                path: '/homes/mycollect',
                name: 'HomesMyCollect',
                meta:{
                    title:'我的收藏'
                },
                component: ()=>import("@/views/home/HomesMyCollect.vue")
            },
            //足迹
            {
                path: '/homes/myfoot',
                name: 'HomesMyFoot',
                meta:{
                    title:'我的足迹'
                },
                component: ()=>import("@/views/home/HomesMyFoot.vue")
            },
            //我买到的订单
            {
                path: '/homes/myBought',
                name: 'HomesMyBought',
                meta:{
                    title:'我买到的订单'
                },
                component: ()=>import("@/views/home/HomesMyBought.vue")
            },
            //我卖出的订单
            {
                path: '/homes/mySold',
                name: 'HomesMySold',
                meta:{
                    title:'我卖出的订单'
                },
                component: ()=>import("@/views/home/HomesMySold.vue")
            },
            //编辑商品
            {
                path: '/homes/goodsEdit',
                name: 'HomesEdit',
                meta:{
                    keepAlive: false,
                    title:'编辑商品'
                },
                component: ()=>import("@/views/home/HomesGoodsEdit.vue")
            },
        ]
    },
    // ========== 原有前台路由（无修改） ==========
    {
        path: '/',
        component: HomePageLayout,
        redirect: '/homes/home',
        children: [
            {
                path: '/homes/home',
                name: 'HomesPages',
                meta:{
                    title:'首页'
                },
                component: ()=>import("@/views/home/HomesPages.vue")
            },
            {
                path: '/homes/login',
                name: 'HomesLogin',
                meta:{
                    title:'登录'
                },
                component: ()=>import("@/views/home/HomersLogin.vue"),
                redirect: '/homes/login/register',
                children: [
                    {
                        path: '/homes/login/register',
                        name: 'HomesRegister',
                        meta:{
                            title:'登录'
                        },
                        component: ()=>import("@/views/home/Login.vue")
                    }
                ]
            },
            {
                path: '/goods/detail/:id',
                name: 'HomesDetail',
                meta:{
                    title:'商品详情'
                },
                component: ()=>import("@/views/home/HomesDetail.vue")
            },
            //地址
             {
                 path: '/homes/address',
                 name: 'HomesAddress',
                 meta:{
                     title:'收货地址'
                 }
                 ,
                 component: ()=>import("@/views/home/HomesAddress.vue")
             },
            //发布商品
            {
                path: '/homes/publish',
                name: 'HomesPublish',
                meta:{
                    keepAlive: false,
                    title:'发布商品'
                },
                component: ()=>import("@/views/home/HomesPublish.vue")
            },
            //通知
            {
                path: '/homes/notice',
                name: 'HomesNotice',
                meta:{
                    title:'消息'
                },
                component: ()=>import("@/views/home/HomesNotice.vue")
            },
            //卖家信息
            {
                path: '/seller/detail/:sellerId',
                name: 'HomesSeller',
                meta:{
                    title:'卖家信息'
                },
                component: ()=>import("@/views/home/HomesSellerDetail.vue")
            },
            // 支付页面
            {
                path: '/payment',
                name: 'PaymentPage',
                meta:{
                    title:'确认订单'
                },
                component: ()=>import("@/views/home/PaymentPage.vue")
            },
            //AI智能助手
            {
                path: '/homes/AIChat',
                name: 'AIPage',
                meta:{
                    title:'AI智能助手'
                },
                component: ()=>import("@/views/home/HomesAIChat.vue")
            },
            //搜索
            {
                path: '/homes/search',
                name: 'HomesSearch',
                meta:{
                    title:'搜索'
                },
                component: ()=>import("@/views/home/HomesSearch.vue")
            },
            // 404 页面（放在最后，兜底匹配所有未定义路径）
            {
                path: '/:pathMatch(.*)*',
                name: 'NotFound',
                meta:{
                    title:'404 - 页面不存在'
                },
                component: ()=>import("@/views/home/NotFound.vue")
            }
        ]
    }
];

// 创建路由器
const router = createRouter({
    history: createWebHistory(),
    routes: routes
});

// 存储每个路由的滚动位置
const scrollPositions = {};
//在调用router时会自动调用beforeEach和afterEach钩子
router.beforeEach((to, from, next) => {
    // 保存当前页面的滚动位置
    if (from.path) {
        scrollPositions[from.path] = window.scrollY || document.documentElement.scrollTop;
    }

    if (to.meta?.title) {
        document.title = to.meta.title;
    }

    // 继续导航
    next();
});

router.afterEach((to) => {
    // 恢复之前保存的滚动位置
    if (scrollPositions[to.path]) {
        window.scrollTo(0, scrollPositions[to.path]);
    } else {
        window.scrollTo(0, 0);
    }
});

router.beforeEach((to, from, next) => {
    // 保存当前页面的滚动位置
    if (from.path) {
        scrollPositions[from.path] = window.scrollY || document.documentElement.scrollTop;
    }
    const loadingStore = useLoadingStore();
    loadingStore.setLoading(true);
    // 继续导航
    next();
});

router.afterEach((to) => {
    // 恢复之前保存的滚动位置
    if (scrollPositions[to.path]) {
        window.scrollTo(0, scrollPositions[to.path]);
    } else {
        window.scrollTo(0, 0);
    }
    const loadingStore = useLoadingStore();
    loadingStore.setLoading(false);
});

export default router;
