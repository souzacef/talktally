import { describe, expect, it } from 'vitest'
import { encodePcm16Wav } from '@/lib/audio/wav'

function ascii(view: DataView, offset: number, length: number): string {
  return Array.from({ length }, (_, index) => String.fromCharCode(view.getUint8(offset + index))).join('')
}

describe('encodePcm16Wav', () => {
  it('writes a mono PCM 16-bit WAV header with little-endian lengths', () => {
    const wav = encodePcm16Wav([new Float32Array([0, 0.5, -0.5])], 48_000)
    const view = new DataView(wav)
    expect(ascii(view, 0, 4)).toBe('RIFF')
    expect(view.getUint32(4, true)).toBe(wav.byteLength - 8)
    expect(ascii(view, 8, 4)).toBe('WAVE')
    expect(ascii(view, 12, 4)).toBe('fmt ')
    expect(view.getUint16(20, true)).toBe(1)
    expect(view.getUint16(22, true)).toBe(1)
    expect(view.getUint32(24, true)).toBe(48_000)
    expect(view.getUint16(34, true)).toBe(16)
    expect(ascii(view, 36, 4)).toBe('data')
    expect(view.getUint32(40, true)).toBe(6)
  })

  it('clips samples and writes signed PCM values', () => {
    const view = new DataView(encodePcm16Wav([
      new Float32Array([-2, -1, 0, 1, 2]),
    ], 16_000))
    expect(view.getInt16(44, true)).toBe(-32_768)
    expect(view.getInt16(46, true)).toBe(-32_768)
    expect(view.getInt16(48, true)).toBe(0)
    expect(view.getInt16(50, true)).toBe(32_767)
    expect(view.getInt16(52, true)).toBe(32_767)
  })

  it('rejects invalid sample rates', () => {
    expect(() => encodePcm16Wav([], 0)).toThrow(/sample rate/)
  })
})
