FROM nginx:1.25-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY . /tmp/app
RUN test -f /tmp/app/unpackage/dist/build/h5/index.html \
    || (echo "Missing app/unpackage/dist/build/h5/index.html. Build the uni-app H5 artifact first, then run: docker compose --profile app-h5 up --build app-h5" && false) \
    && cp -a /tmp/app/unpackage/dist/build/h5/. /usr/share/nginx/html/ \
    && rm -rf /tmp/app
EXPOSE 80
