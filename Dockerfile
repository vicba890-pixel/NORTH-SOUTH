# Use official lightweight Node.js Alpine image
FROM node:18-alpine

# Set working directory inside container
WORKDIR /usr/src/app

# Copy package descriptors and install only production dependencies
COPY package*.json ./
RUN npm install --only=production

# Copy server logic and static web/PWA folder
COPY server.js ./
COPY public/ ./public/

# Expose server listener port
EXPOSE 8080

# Configure environment variables
ENV PORT=8080
ENV NODE_ENV=production

# Run start script on container initialization
CMD ["npm", "start"]
