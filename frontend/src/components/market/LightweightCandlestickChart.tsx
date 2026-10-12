import { useEffect, useMemo, useRef, useState } from 'react'
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  createChart,
  type CandlestickData,
  type IChartApi,
  type ISeriesApi,
  type Time,
  type UTCTimestamp,
} from 'lightweight-charts'

import type { Candle } from '../../api/market'

interface LightweightCandlestickChartProps {
  candles: Candle[]
}

interface CandleDetails {
  timestamp: string
  open: number
  high: number
  low: number
  close: number
}

function toUtcTimestamp(timestamp: string): UTCTimestamp {
  return Math.floor(new Date(timestamp).getTime() / 1000) as UTCTimestamp
}

function formatTime(time: Time) {
  if (typeof time === 'number') {
    return new Date(time * 1000).toISOString()
  }
  if (typeof time === 'string') {
    return new Date(`${time}T00:00:00Z`).toISOString()
  }
  return new Date(Date.UTC(time.year, time.month - 1, time.day)).toISOString()
}

function toChartData(candles: Candle[]): CandlestickData<Time>[] {
  const candlesByTimestamp = new Map<number, CandlestickData<Time>>()

  candles.forEach((candle) => {
    const time = toUtcTimestamp(candle.timestamp)
    if (Number.isFinite(time)) {
      candlesByTimestamp.set(time, {
        time,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close,
      })
    }
  })

  return [...candlesByTimestamp.values()].sort(
    (left, right) => Number(left.time) - Number(right.time),
  )
}

export function LightweightCandlestickChart({
  candles,
}: LightweightCandlestickChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null)
  const chartData = useMemo(() => toChartData(candles), [candles])
  const [hoveredCandle, setHoveredCandle] = useState<CandleDetails | null>(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, {
      width: container.clientWidth,
      height: container.clientHeight || 420,
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: '#94a3b8',
        attributionLogo: true,
      },
      grid: {
        vertLines: { color: 'rgba(148, 163, 184, 0.12)' },
        horzLines: { color: 'rgba(148, 163, 184, 0.12)' },
      },
      crosshair: { mode: CrosshairMode.Normal },
      rightPriceScale: { borderColor: 'rgba(148, 163, 184, 0.25)' },
      timeScale: {
        borderColor: 'rgba(148, 163, 184, 0.25)',
        timeVisible: true,
        secondsVisible: false,
      },
    })
    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#14b8a6',
      downColor: '#f43f5e',
      borderVisible: false,
      wickUpColor: '#14b8a6',
      wickDownColor: '#f43f5e',
    })

    const handleCrosshairMove = (parameter: Parameters<IChartApi['subscribeCrosshairMove']>[0] extends (value: infer Value) => void ? Value : never) => {
      if (!parameter.time) {
        setHoveredCandle(null)
        return
      }
      const point = parameter.seriesData.get(series)
      if (!point || !('open' in point)) {
        setHoveredCandle(null)
        return
      }
      setHoveredCandle({
        timestamp: formatTime(parameter.time),
        open: point.open,
        high: point.high,
        low: point.low,
        close: point.close,
      })
    }

    chart.subscribeCrosshairMove(handleCrosshairMove)
    const resizeObserver = new ResizeObserver(([entry]) => {
      if (!entry) return
      chart.applyOptions({
        width: Math.floor(entry.contentRect.width),
        height: Math.floor(entry.contentRect.height),
      })
    })
    resizeObserver.observe(container)

    chartRef.current = chart
    seriesRef.current = series

    return () => {
      chart.unsubscribeCrosshairMove(handleCrosshairMove)
      resizeObserver.disconnect()
      chart.remove()
      chartRef.current = null
      seriesRef.current = null
    }
  }, [])

  useEffect(() => {
    seriesRef.current?.setData(chartData)
    chartRef.current?.timeScale().fitContent()
  }, [chartData])

  const latestCandle = candles.at(-1)
  const details = hoveredCandle ?? (latestCandle
    ? {
        timestamp: latestCandle.timestamp,
        open: latestCandle.open,
        high: latestCandle.high,
        low: latestCandle.low,
        close: latestCandle.close,
      }
    : null)

  return (
    <div>
      {details ? (
        <dl aria-label="Candle details" className="mb-4 grid grid-cols-2 gap-3 text-xs sm:grid-cols-5">
          {(['open', 'high', 'low', 'close'] as const).map((field) => (
            <div key={field}>
              <dt className="font-semibold uppercase tracking-wider text-slate-400">{field}</dt>
              <dd className="mt-1 font-mono text-slate-700 dark:text-slate-200">{details[field]}</dd>
            </div>
          ))}
          <div className="col-span-2 sm:col-span-1">
            <dt className="font-semibold uppercase tracking-wider text-slate-400">Timestamp</dt>
            <dd className="mt-1 font-mono text-slate-700 dark:text-slate-200">
              <time dateTime={details.timestamp}>{details.timestamp}</time>
            </dd>
          </div>
        </dl>
      ) : null}
      <div
        ref={containerRef}
        aria-label="Forex candlestick chart"
        className="h-[22rem] min-w-0 w-full md:h-[28rem]"
      />
    </div>
  )
}
