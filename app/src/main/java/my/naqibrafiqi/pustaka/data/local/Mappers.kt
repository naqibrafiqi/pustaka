package my.naqibrafiqi.pustaka.data.local

import my.naqibrafiqi.pustaka.data.local.entity.BookEntity
import my.naqibrafiqi.pustaka.data.local.entity.HighlightEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingProgressEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingSessionEntity
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingSession
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.model.SeriesDetails

internal fun SeriesEntity.toModel() = Series(
    id = id,
    folderPath = folderPath,
    folderName = folderName,
    editedName = editedName,
    sourceDetails = source ?: SeriesDetails.EMPTY,
    editedDetails = edited ?: SeriesDetails.EMPTY,
    isHidden = isHidden,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Series.toEntity() = SeriesEntity(
    id = id,
    folderPath = folderPath,
    folderName = folderName,
    editedName = editedName,
    source = sourceDetails,
    edited = editedDetails,
    isHidden = isHidden,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun BookEntity.toModel() = Book(
    id = id,
    seriesId = seriesId,
    filePath = filePath,
    fileName = fileName,
    fingerprint = fingerprint,
    fileSize = fileSize,
    fileLastModified = fileLastModified,
    isMissing = isMissing,
    isHidden = isHidden,
    editedName = editedName,
    sourceDetails = source ?: BookDetails.EMPTY,
    editedDetails = edited ?: BookDetails.EMPTY,
    totalPositions = totalPositions,
    readingStatus = readingStatus,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
)

internal fun Book.toEntity() = BookEntity(
    id = id,
    seriesId = seriesId,
    filePath = filePath,
    fileName = fileName,
    fingerprint = fingerprint,
    fileSize = fileSize,
    fileLastModified = fileLastModified,
    isMissing = isMissing,
    isHidden = isHidden,
    editedName = editedName,
    source = sourceDetails,
    edited = editedDetails,
    totalPositions = totalPositions,
    readingStatus = readingStatus,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
)

internal fun ReadingProgressEntity.toModel() = ReadingProgress(
    bookId = bookId,
    locator = locator,
    progression = progression,
    position = position,
    updatedAt = updatedAt,
)

internal fun ReadingSessionEntity.toModel() = ReadingSession(
    id = id,
    bookId = bookId,
    startedAt = startedAt,
    endedAt = endedAt,
    pagesRead = pagesRead,
)

internal fun HighlightEntity.toModel() = Highlight(
    id = id,
    bookId = bookId,
    locator = locator,
    text = text,
    note = note,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Highlight.toEntity() = HighlightEntity(
    id = id,
    bookId = bookId,
    locator = locator,
    text = text,
    note = note,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
