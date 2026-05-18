FROM node:14-bullseye AS build

WORKDIR /workspace/app
COPY package*.json ./
RUN npm install -g npm@8.19.4 \
    && npm install --legacy-peer-deps
COPY . .
RUN npm run patch:uni \
    && npm run build:h5

FROM nginx:1.25-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/app/unpackage/dist/build/h5/ /usr/share/nginx/html/
EXPOSE 80
