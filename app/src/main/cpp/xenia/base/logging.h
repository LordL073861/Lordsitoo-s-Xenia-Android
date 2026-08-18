#pragma once

#include <string>
#include <vector>
#include <mutex>
#include <android/log.h>

#define XE_LOG_TAG "XeniaNative"

namespace xe {

enum class LogLevel {
    Debug,
    Info,
    Warn,
    Error
};

struct LogMessage {
    std::string tag;
    LogLevel level;
    std::string message;
    uint64_t timestamp_ms;
};

class Logger {
public:
    static Logger& Get();

    void Log(const char* tag, LogLevel level, const char* format, ...);
    std::vector<std::string> GetRecentLogs();
    void Clear();

private:
    Logger() = default;
    std::mutex mutex_;
    std::vector<LogMessage> log_buffer_;
    const size_t max_buffer_size_ = 500;
};

#define XELOGD(tag, fmt, ...) ::xe::Logger::Get().Log(tag, ::xe::LogLevel::Debug, fmt, ##__VA_ARGS__)
#define XELOGI(tag, fmt, ...) ::xe::Logger::Get().Log(tag, ::xe::LogLevel::Info, fmt, ##__VA_ARGS__)
#define XELOGW(tag, fmt, ...) ::xe::Logger::Get().Log(tag, ::xe::LogLevel::Warn, fmt, ##__VA_ARGS__)
#define XELOGE(tag, fmt, ...) ::xe::Logger::Get().Log(tag, ::xe::LogLevel::Error, fmt, ##__VA_ARGS__)

}  // namespace xe
