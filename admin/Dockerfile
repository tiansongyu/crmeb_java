FROM node:14-bullseye AS build

WORKDIR /workspace
COPY package*.json ./
RUN npm install -g npm@9.9.4 \
    && if [ -f package-lock.json ]; then npm ci --legacy-peer-deps; else npm install --legacy-peer-deps; fi
COPY . .
RUN npm run test:unit -- --runInBand \
    && npm run build:prod

FROM nginx:1.25-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
