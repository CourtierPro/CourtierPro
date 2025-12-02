FROM node:20 AS build
WORKDIR /app

# copy package files first (better cache + clean install)
COPY package*.json ./

# install INCLUDING devDependencies (tsc lives there)
RUN npm ci

# now copy the rest of the app
COPY . .

# build
RUN npm run build



FROM nginx:alpine

# replace the configuration by default
COPY nginx.conf /etc/nginx/conf.d/default.conf

# copie you builde Vite
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
