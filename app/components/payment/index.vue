<template>
	<view>
		<view class="payment" :class="pay_close ? 'on' : ''">
			<view class="title acea-row row-center-wrapper">
				选择付款方式<text class="iconfont icon-guanbi" @click="close"></text>
			</view>
			<view class="item acea-row row-between-wrapper" @click="goPay(item.value)"
				v-for="(item,index) in payMode" :key="index" v-if="item.payStatus == 1">
				<view class="left acea-row row-between-wrapper">
					<view class="iconfont" :class="item.icon"></view>
					<view class="text">
						<view class="name">{{item.name}}</view>
						<view class="info">{{item.title}}</view>
					</view>
				</view>
				<view class="iconfont icon-xiangyou"></view>
			</view>
			<view class="empty" v-if="!hasPayMode">暂无支付方式</view>
		</view>
		<view class="mask" @click="close" v-if="pay_close"></view>
	</view>
</template>

<script>
	import {
		getOrderPayConfig
	} from '@/api/order.js';

	export default {
		props: {
			pay_close: {
				type: Boolean,
				default: false,
			},
			order_id: {
				type: String,
				default: ''
			},
			totalPrice: {
				type: String,
				default: '0'
			}
		},
		data() {
			return {
				payMode: [{
					name: '扫码转账',
					icon: 'icon-yuezhifu1',
					value: 'offline',
					title: '上传付款凭证后等待确认',
					payStatus: 1
				}]
			};
		},
		computed: {
			hasPayMode() {
				return this.payMode.some(item => item.payStatus == 1);
			}
		},
		created() {
			this.payConfig();
		},
		methods: {
			close() {
				this.$emit('onChangeFun', {
					action: 'payClose'
				});
			},
			payConfig() {
				getOrderPayConfig().then(res => {
					this.payMode[0].payStatus = res.data.offlinePayStatus ? 1 : 0;
				});
			},
			goPay() {
				if (!this.order_id) {
					return this.$util.Tips({
						title: '请选择要支付的订单'
					});
				}
				this.close();
				uni.navigateTo({
					url: '/pages/order/order_payment/index?orderNo=' + this.order_id + '&payPrice=' + this.totalPrice
				});
			}
		}
	}
</script>

<style scoped lang="scss">
	.payment {
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		border-radius: 16rpx 16rpx 0 0;
		background-color: #fff;
		padding-bottom: 60rpx;
		z-index: 99;
		transition: all 0.3s cubic-bezier(0.25, 0.5, 0.5, 0.9);
		transform: translate3d(0, 100%, 0);
	}

	.payment.on {
		transform: translate3d(0, 0, 0);
	}

	.payment .title {
		text-align: center;
		height: 123rpx;
		font-size: 32rpx;
		color: #282828;
		font-weight: bold;
		padding-right: 30rpx;
		margin-left: 30rpx;
		position: relative;
		border-bottom: 1rpx solid #eee;
	}

	.payment .title .iconfont {
		position: absolute;
		right: 30rpx;
		top: 50%;
		transform: translateY(-50%);
		font-size: 43rpx;
		color: #8a8a8a;
		font-weight: normal;
	}

	.payment .item {
		border-bottom: 1rpx solid #eee;
		height: 130rpx;
		margin-left: 30rpx;
		padding-right: 30rpx;
	}

	.payment .item .left {
		width: 610rpx;
	}

	.payment .item .left .text {
		width: 540rpx;
	}

	.payment .item .left .text .name {
		font-size: 32rpx;
		color: #282828;
	}

	.payment .item .left .text .info,
	.payment .empty {
		font-size: 24rpx;
		color: #999;
	}

	.payment .item .left .iconfont {
		font-size: 45rpx;
		color: #eb6623;
	}

	.payment .item .iconfont {
		font-size: 30rpx;
		color: #999;
	}

	.payment .empty {
		padding: 40rpx 30rpx 0;
		text-align: center;
	}

	.mask {
		z-index: 98;
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.45);
	}
</style>
