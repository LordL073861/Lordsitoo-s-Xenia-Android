#include "logging.h"
#include <cstdio>
#include <cstdarg>
#include <chrono>

namespace xe {

Logger& Logger::Get() {
    static Logger instance;
    return instance;
}

void Logger::Log(const char* tag, LogLevel level, const char* format, ...) {
    char buffer[2048];
    va_list args;
    va_start(args, format);
    vsnprintf(buffer, sizeof(buffer), format, args);
    va_end(args);

    android_LogPriority prio = ANDROID_LOG_INFO;
    switch (level) {
        case LogLevel::Debug: prio = ANDROID_LOG_DEBUG; break;
        case LogLevel::Info: prio = ANDROID_LOG_INFO; break;
        case LogLevel::Warn: prio = ANDROID_LOG_WARN; break;
        case LogLevel::Error: prio = ANDROID_LOG_ERROR; break;
    }

    __android_log_print(prio, tag ? tag : XE_LOG_TAG, "%s", buffer);

    auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();

    std::lock_guard<std::mutex> lock(mutex_);
    if (log_buffer_.size() >= max_buffer_size_) {
        log_buffer_.erase(log_buffer_.begin());
    }
    log_buffer_.push_back({tag ? tag : "SYS", level, buffer, static_cast<uint64_t>(now)});
}

std::vector<std::string> Logger::GetRecentLogs() {
    std::lock_guard<std::mutex> lock(mutex_);
    std::vector<std::string> result;
    result.reserve(log_buffer_.size());
    for (const auto& entry : log_buffer_) {
        const char* lvlStr = "I";
        if (entry.level == LogLevel::Debug) lvlStr = "D";
        else if (entry.level == LogLevel::Warn) lvlStr = "W";
        else if (entry.level == LogLevel::Error) lvlStr = "E";

        char formatted[2200];
        snprintf(formatted, sizeof(formatted), "[%s] [%s] %s", lvlStr, entry.tag.c_str(), entry.message.c_str());
        result.push_back(formatted);
    }
    return result;
}

void Logger::Clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    log_buffer_.clear();
}

}  // namespace xe
