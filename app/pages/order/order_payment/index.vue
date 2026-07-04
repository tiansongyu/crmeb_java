<template>
	<view :data-theme="theme" class="manual-pay-page">
		<view class="amount-panel">
			<view class="label">订单金额</view>
			<view class="amount">￥<text>{{payPrice}}</text></view>
			<view class="order-no">{{orderNo}}</view>
		</view>

		<view class="section">
			<view class="section-title">扫码转账</view>
			<view class="qrcode-wrap" @tap="previewQrcode">
				<image v-if="offlinePayQrcode" :src="offlinePayQrcode" mode="aspectFit"></image>
				<view v-else class="qrcode-empty">待配置收款码</view>
			</view>
			<view class="pay-name" v-if="offlinePayName">{{offlinePayName}}</view>
			<view class="pay-tips" v-if="offlinePayTips">{{offlinePayTips}}</view>
		</view>

		<view class="section">
			<view class="section-title">付款凭证</view>
			<view class="upload-box" v-if="!voucher" @tap="uploadProof">
				<view class="upload-icon">+</view>
				<view>上传支付订单截图</view>
			</view>
			<view class="proof-preview" v-else>
				<image :src="voucher" mode="aspectFill" @tap="previewVoucher"></image>
				<view class="remove" @tap.stop="removeVoucher">删除</view>
			</view>
			<view class="form-row">
				<input v-model="tradeNo" placeholder="付款交易号（选填）" placeholder-style="color:#bbb;" />
			</view>
			<view class="form-row textarea-row">
				<textarea v-model="remark" placeholder="付款备注（选填）" placeholder-style="color:#bbb;" maxlength="200"></textarea>
			</view>
		</view>

		<view class="footer">
			<button class="submit bg-color" :disabled="submitting" @tap="submitProof">
				{{submitting ? '提交中' : '提交付款凭证'}}
			</button>
		</view>
	</view>
</template>

<script>
	import {
		orderPay,
		offlinePayProof
	} from '@/api/order.js';
	import {
		mapGetters
	} from "vuex";
	let app = getApp();

	export default {
		data() {
			return {
				theme: app.globalData.theme,
				orderNo: '',
				payPrice: '0',
				offlinePayQrcode: '',
				offlinePayName: '',
				offlinePayTips: '',
				voucher: '',
				tradeNo: '',
				remark: '',
				submitting: false
			}
		},
		computed: mapGetters(['productType']),
		onLoad(options) {
			this.orderNo = options.orderNo || '';
			this.payPrice = options.payPrice || '0';
			if (!this.orderNo) {
				return this.$util.Tips({
					title: '缺少订单号'
				}, {
					tab: 5,
					url: '/pages/users/order_list/index'
				});
			}
			this.initOfflinePay();
		},
		methods: {
			initOfflinePay() {
				uni.showLoading({
					title: '正在加载'
				});
				orderPay({
					orderNo: this.orderNo,
					payChannel: 'offline',
					payType: 'offline',
					scene: this.productType === 'normal' ? 0 : 1177
				}).then(res => {
					uni.hideLoading();
					const data = res.data || {};
					this.orderNo = data.orderNo || this.orderNo;
					this.offlinePayQrcode = data.offlinePayQrcode || '';
					this.offlinePayName = data.offlinePayName || '';
					this.offlinePayTips = data.offlinePayTips || '';
				}).catch(err => {
					uni.hideLoading();
					this.$util.Tips({
						title: err
					});
				});
			},
			uploadProof() {
				this.$util.uploadImageOne({
					url: 'upload/image',
					name: 'multipart',
					model: 'order',
					pid: 1
				}, res => {
					this.voucher = res.data.url;
				});
			},
			removeVoucher() {
				this.voucher = '';
			},
			previewQrcode() {
				if (!this.offlinePayQrcode) return;
				uni.previewImage({
					urls: [this.offlinePayQrcode]
				});
			},
			previewVoucher() {
				if (!this.voucher) return;
				uni.previewImage({
					urls: [this.voucher]
				});
			},
			submitProof() {
				if (!this.voucher) {
					return this.$util.Tips({
						title: '请上传支付订单截图'
					});
				}
				this.submitting = true;
				offlinePayProof({
					orderNo: this.orderNo,
					voucher: this.voucher,
					tradeNo: this.tradeNo,
					remark: this.remark
				}).then(() => {
					this.submitting = false;
					this.$util.Tips({
						title: '提交成功',
						icon: 'success'
					}, {
						tab: 5,
						url: '/pages/order/order_pay_status/index?order_id=' + this.orderNo + '&status=3'
					});
				}).catch(err => {
					this.submitting = false;
					this.$util.Tips({
						title: err
					});
				});
			}
		}
	}
</script>

<style lang="scss" scoped>
	.manual-pay-page {
		min-height: 100vh;
		background: #f5f5f5;
		padding: 24rpx 24rpx 160rpx;
		box-sizing: border-box;
	}

	.amount-panel,
	.section {
		background: #fff;
		border-radius: 14rpx;
		padding: 32rpx;
		margin-bottom: 24rpx;
	}

	.amount-panel {
		text-align: center;
	}

	.label,
	.order-no,
	.pay-tips,
	.upload-box,
	.form-row input,
	.form-row textarea {
		color: #777;
		font-size: 26rpx;
	}

	.amount {
		margin-top: 12rpx;
		color: #e93323;
		font-size: 36rpx;
		font-weight: 700;
	}

	.amount text {
		font-size: 60rpx;
	}

	.order-no {
		margin-top: 12rpx;
		word-break: break-all;
	}

	.section-title {
		color: #282828;
		font-size: 32rpx;
		font-weight: 700;
		margin-bottom: 28rpx;
	}

	.qrcode-wrap {
		width: 430rpx;
		height: 430rpx;
		margin: 0 auto;
		border: 1rpx solid #eee;
		border-radius: 12rpx;
		background: #fafafa;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.qrcode-wrap image {
		width: 100%;
		height: 100%;
	}

	.qrcode-empty {
		color: #999;
		font-size: 28rpx;
	}

	.pay-name {
		margin-top: 22rpx;
		text-align: center;
		color: #333;
		font-size: 30rpx;
		font-weight: 600;
	}

	.pay-tips {
		margin-top: 22rpx;
		line-height: 1.6;
	}

	.upload-box {
		height: 220rpx;
		border: 1rpx dashed #d8d8d8;
		border-radius: 12rpx;
		background: #fafafa;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.upload-icon {
		width: 64rpx;
		height: 64rpx;
		line-height: 58rpx;
		text-align: center;
		border-radius: 50%;
		border: 2rpx solid #ddd;
		color: #999;
		font-size: 52rpx;
		margin-bottom: 12rpx;
	}

	.proof-preview {
		position: relative;
		width: 220rpx;
		height: 220rpx;
		border-radius: 12rpx;
		overflow: hidden;
	}

	.proof-preview image {
		width: 100%;
		height: 100%;
	}

	.proof-preview .remove {
		position: absolute;
		right: 0;
		bottom: 0;
		left: 0;
		height: 52rpx;
		line-height: 52rpx;
		text-align: center;
		background: rgba(0, 0, 0, .55);
		color: #fff;
		font-size: 24rpx;
	}

	.form-row {
		margin-top: 24rpx;
		border-bottom: 1rpx solid #eee;
	}

	.form-row input {
		height: 78rpx;
	}

	.form-row textarea {
		width: 100%;
		height: 140rpx;
		padding-top: 20rpx;
		box-sizing: border-box;
	}

	.footer {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: 18rpx 30rpx 34rpx;
		background: #fff;
		box-shadow: 0 -6rpx 18rpx rgba(0, 0, 0, .04);
	}

	.submit {
		height: 86rpx;
		line-height: 86rpx;
		border-radius: 43rpx;
		color: #fff;
		font-size: 30rpx;
		font-weight: 700;
	}
</style>
