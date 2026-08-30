FROM golang:1.23-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o rabotyaga-agent .

FROM alpine:3.20
RUN apk add --no-cache ca-certificates docker-cli git
WORKDIR /app
COPY --from=builder /app/rabotyaga-agent /usr/local/bin/rabotyaga-agent

ENTRYPOINT ["rabotyaga-agent"]
CMD ["run"]
