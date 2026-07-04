FROM node:14-bullseye AS build

WORKDIR /workspace/app
ARG VUE_APP_IMAGE_DOMAIN=https://apia.beta.crmeb.xbdzz.cn
ENV VUE_APP_IMAGE_DOMAIN=${VUE_APP_IMAGE_DOMAIN}
COPY package*.json ./
RUN npm install -g npm@8.19.4 \
    && npm install --legacy-peer-deps
COPY . .
RUN npm run patch:uni \
    && npm run build:h5

FROM nginx:1.25-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/app/unpackage/dist/build/h5/ /usr/share/nginx/html/
# uni-app H5 build 在 docker 内 vue-cli-service 跑 production 时不会自动把 dist/static/index.css link 到 index.html，
# 这导致 uni-view/uni-text 等元素失去默认 display:flex，渲染时多个 DIY 组件高度变 0。
# 在镜像构建期手动注入 <link> 标签，确保所有 uni 元素有正确的默认样式。
RUN sed -i 's|<head>|<head><link rel="stylesheet" href="/static/index.css">|' /usr/share/nginx/html/index.html
EXPOSE 80
