const WAV_HEADER_BYTES = 44

function writeAscii(view: DataView, offset: number, value: string): void {
  for (let index = 0; index < value.length; index += 1) {
    view.setUint8(offset + index, value.charCodeAt(index))
  }
}

export function encodePcm16Wav(
  chunks: readonly Float32Array[],
  sampleRate: number,
): ArrayBuffer {
  if (!Number.isInteger(sampleRate) || sampleRate <= 0) {
    throw new Error('sample rate must be a positive integer')
  }

  const sampleCount = chunks.reduce((total, chunk) => total + chunk.length, 0)
  const dataBytes = sampleCount * 2
  const buffer = new ArrayBuffer(WAV_HEADER_BYTES + dataBytes)
  const view = new DataView(buffer)

  writeAscii(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataBytes, true)
  writeAscii(view, 8, 'WAVE')
  writeAscii(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeAscii(view, 36, 'data')
  view.setUint32(40, dataBytes, true)

  let offset = WAV_HEADER_BYTES
  chunks.forEach((chunk) => {
    chunk.forEach((sample) => {
      const clipped = Math.max(-1, Math.min(1, sample))
      const pcm = clipped < 0 ? clipped * 0x8000 : clipped * 0x7fff
      view.setInt16(offset, Math.round(pcm), true)
      offset += 2
    })
  })

  return buffer
}
