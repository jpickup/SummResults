# build stage
# Pin Node to avoid breakage when `node:lts-alpine` advances (e.g. to Node 24+).
FROM docker.io/library/node:22-alpine as build-stage
WORKDIR /app
COPY package*.json ./
RUN apk add --update python3 py3-pip py3-distutils-extra make g++\
   && rm -rf /var/cache/apk/*
# Install deps (lockfile may be updated automatically if package.json changed)
RUN npm install
COPY . .
RUN npm run build

# production stage
FROM docker.io/library/nginx:stable-alpine as production-stage

COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
#CMD ["nginx-debug", "-g", "daemon off;"]