package com.solana.solmind.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OnchainIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier.size(size)) {
        drawOnchainIcon(color)
    }
}

@Composable
fun OffchainIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier.size(size)) {
        drawOffchainIcon(color)
    }
}

private fun DrawScope.drawOnchainIcon(color: Color) {
    val strokeWidth = size.width * 0.08f
    val blockSize = size.width * 0.15f
    val spacing = size.width * 0.08f
    
    // Draw blockchain blocks
    val blocks = listOf(
        Offset(size.width * 0.1f, size.height * 0.4f),
        Offset(size.width * 0.35f, size.height * 0.25f),
        Offset(size.width * 0.6f, size.height * 0.5f),
        Offset(size.width * 0.8f, size.height * 0.35f)
    )
    
    // Draw connecting lines first
    for (i in 0 until blocks.size - 1) {
        drawLine(
            color = color,
            start = Offset(blocks[i].x + blockSize, blocks[i].y + blockSize/2),
            end = Offset(blocks[i+1].x, blocks[i+1].y + blockSize/2),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
    }
    
    // Draw network connections (dotted lines)
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(blocks[0].x + blockSize/2, blocks[0].y - spacing),
        end = Offset(blocks[1].x + blockSize/2, blocks[1].y - spacing),
        strokeWidth = strokeWidth * 0.5f,
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
    
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(blocks[1].x + blockSize/2, blocks[1].y + blockSize + spacing),
        end = Offset(blocks[2].x + blockSize/2, blocks[2].y + blockSize + spacing),
        strokeWidth = strokeWidth * 0.5f,
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
    
    // Draw blocks
    blocks.forEach { position ->
        // Block outline
        drawRoundRect(
            color = color,
            topLeft = position,
            size = Size(blockSize, blockSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth),
            style = Stroke(width = strokeWidth)
        )
        
        // Block center dot (representing data)
        drawCircle(
            color = color,
            radius = strokeWidth * 0.8f,
            center = Offset(position.x + blockSize/2, position.y + blockSize/2)
        )
    }
}

private fun DrawScope.drawOffchainIcon(color: Color) {
    val strokeWidth = size.width * 0.08f
    
    // Draw computer/device outline
    val screenWidth = size.width * 0.6f
    val screenHeight = size.height * 0.45f
    val screenX = (size.width - screenWidth) / 2
    val screenY = size.height * 0.15f
    
    // Screen
    drawRoundRect(
        color = color,
        topLeft = Offset(screenX, screenY),
        size = Size(screenWidth, screenHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth),
        style = Stroke(width = strokeWidth)
    )
    
    // Screen content (representing local data)
    val contentPadding = strokeWidth * 2
    drawRect(
        color = color.copy(alpha = 0.7f),
        topLeft = Offset(screenX + contentPadding, screenY + contentPadding),
        size = Size(screenWidth * 0.4f, strokeWidth * 1.5f)
    )
    
    drawRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(screenX + contentPadding, screenY + contentPadding + strokeWidth * 2.5f),
        size = Size(screenWidth * 0.6f, strokeWidth)
    )
    
    drawRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(screenX + contentPadding, screenY + contentPadding + strokeWidth * 4f),
        size = Size(screenWidth * 0.5f, strokeWidth)
    )
    
    // CPU/Processor indicator
    val cpuSize = strokeWidth * 3f
    val cpuX = screenX + screenWidth - contentPadding - cpuSize
    val cpuY = screenY + contentPadding
    
    drawRoundRect(
        color = color,
        topLeft = Offset(cpuX, cpuY),
        size = Size(cpuSize, cpuSize),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth * 0.3f),
        style = Stroke(width = strokeWidth * 0.8f)
    )
    
    drawCircle(
        color = color,
        radius = strokeWidth * 0.5f,
        center = Offset(cpuX + cpuSize/2, cpuY + cpuSize/2)
    )
    
    // Base/Stand
    val baseWidth = screenWidth * 0.3f
    val baseX = (size.width - baseWidth) / 2
    val baseY = screenY + screenHeight + strokeWidth
    
    drawRect(
        color = color,
        topLeft = Offset(baseX, baseY),
        size = Size(baseWidth, strokeWidth * 0.8f)
    )
    
    val standWidth = screenWidth * 0.5f
    val standX = (size.width - standWidth) / 2
    val standY = baseY + strokeWidth
    
    drawRoundRect(
        color = color,
        topLeft = Offset(standX, standY),
        size = Size(standWidth, strokeWidth * 1.2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth * 0.6f)
    )
    
    // Local storage indicator (small drive)
    val driveSize = strokeWidth * 2.5f
    val driveX = size.width * 0.05f
    val driveY = size.height * 0.8f
    
    drawRoundRect(
        color = color,
        topLeft = Offset(driveX, driveY),
        size = Size(driveSize * 1.5f, driveSize),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth * 0.2f),
        style = Stroke(width = strokeWidth * 0.8f)
    )
    
    drawCircle(
        color = color,
        radius = strokeWidth * 0.3f,
        center = Offset(driveX + strokeWidth, driveY + driveSize/2)
    )
    
    // Offline indicator (disconnected symbol)
    val disconnectX = size.width * 0.8f
    val disconnectY = size.height * 0.8f
    val disconnectSize = strokeWidth * 1.5f
    
    // X mark for offline
    drawLine(
        color = color.copy(alpha = 0.6f),
        start = Offset(disconnectX, disconnectY),
        end = Offset(disconnectX + disconnectSize, disconnectY + disconnectSize),
        strokeWidth = strokeWidth * 0.8f,
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = color.copy(alpha = 0.6f),
        start = Offset(disconnectX + disconnectSize, disconnectY),
        end = Offset(disconnectX, disconnectY + disconnectSize),
        strokeWidth = strokeWidth * 0.8f,
        cap = StrokeCap.Round
    )
}