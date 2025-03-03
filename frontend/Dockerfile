# Build stage
FROM node:18-alpine as build
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Run stage
FROM node:18-alpine
WORKDIR /app

# Install serve to run the application
RUN npm install -g serve

# Copy the build from build stage
COPY --from=build /app/build ./build

EXPOSE 3000

# Start the application
CMD ["serve", "-s", "build", "-l", "3000"]