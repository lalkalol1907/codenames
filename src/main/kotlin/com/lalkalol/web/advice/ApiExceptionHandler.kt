package com.lalkalol.web.advice

import com.lalkalol.game.service.GameException
import com.lalkalol.i18n.LocaleSupport
import com.lalkalol.i18n.Messages
import com.lalkalol.i18n.UiLocale
import com.lalkalol.room.service.RoomException
import com.lalkalol.web.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionHandler(
    private val localeSupport: LocaleSupport,
) {
    @ExceptionHandler(RoomException::class)
    fun handleRoomException(
        request: HttpServletRequest,
        ex: RoomException,
    ): ResponseEntity<ErrorResponse> {
        val locale = localeSupport.resolve(request)
        return ResponseEntity.badRequest().body(
            ErrorResponse(Messages.translateException(locale, ex.message)),
        )
    }

    @ExceptionHandler(GameException::class)
    fun handleGameException(
        request: HttpServletRequest,
        ex: GameException,
    ): ResponseEntity<ErrorResponse> {
        val locale = localeSupport.resolve(request)
        return ResponseEntity.badRequest().body(
            ErrorResponse(Messages.translateException(locale, ex.message)),
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}

fun errorResponse(
    localeSupport: LocaleSupport,
    request: HttpServletRequest,
    errorKey: String,
    status: HttpStatus = HttpStatus.BAD_REQUEST,
): ResponseEntity<ErrorResponse> {
    val locale = localeSupport.resolve(request)
    return ResponseEntity.status(status).body(ErrorResponse(Messages.t(locale, errorKey)))
}

fun exceptionResponse(
    localeSupport: LocaleSupport,
    request: HttpServletRequest,
    message: String?,
    status: HttpStatus = HttpStatus.BAD_REQUEST,
): ResponseEntity<ErrorResponse> {
    val locale = localeSupport.resolve(request)
    return ResponseEntity.status(status).body(
        ErrorResponse(Messages.translateException(locale, message)),
    )
}
