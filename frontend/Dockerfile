# Build stage
FROM node:22-alpine AS build-stage
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:stable-alpine AS production-stage

COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

# At startup, replace the __API_BASE_URL__ placeholder in the built index.html
# with the value of the API_BASE_URL environment variable, then start nginx.
CMD ["/bin/sh", "-c", \
  "sed -i \"s|__API_BASE_URL__|${API_BASE_URL:-http://localhost:8092}|g\" /usr/share/nginx/html/index.html && \
   nginx -g 'daemon off;'"]
