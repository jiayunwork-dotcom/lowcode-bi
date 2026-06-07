FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY .npmrc ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist .

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
