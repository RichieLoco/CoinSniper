# Coin Sniper

## Description

Based on similar trading logic written into the tool here: https://github.com/CyberPunkMetalHead/new-listings-trading-bot (taken from the excellent article https://medium.com/coinmonks/i-made-an-open-source-trading-bot-that-trades-new-coin-listings-within-seconds-a529402b2cdd).

History tells us that coins newly announced on Binance will enjoy a short-term spike in buying activity, causing a sharp rise in price(!!), and this bot aims to get a piece of that price action!  

This bot will poll Binance's Announcement API and filter down chosen exchanges (explained in the next section) to a single exchange (within our control) where we can place the trade.  

As an extension to the original source of the idea, this bot will also poll for coin de-listings on Binance, following up with shorting that coin on chosen exchanges using similarly derived AI-based filtration rules.

## AI-based filtering

Chosen exchanges are then filtered down with a number of chatGPT calls that will assess the risk of trading on that exchange versus a more stable and liquid exchange (like Binance).  It will compare based on the following factors:
- latency difference (as we will place trades using the chosen exchange's APIs)
- trading fee difference
- liquidity difference

### Filtering breakdown

...from...

\<List of supported trading exchanges> (defined in our config)

...to...

\<Matched exchanges listing new Binance-announced coin>


...from...

\<Multiple ChatGPT-based risk assessment calls on matched exchanges>

...to...

\<A single exchange that we will trade with>

...to...

\<Persistence store>

...to...

\<Deep learning model>


## Persistence

The aim is for this lightweight bot to be left running unattended, and so, to keep up with its decisions and trading activity, we will look to persist these to keep an up-to-date record of events.

## Deep Learning (Aspirational Goal...)

Using Java Deep Library (JDL) (see https://djl.ai/), the performance of the bot's past decisions and outcomes will be taken into account

Java Deep Learning Library (DJL) (see https://djl.ai/) will be integrated into this trading bot to train reinforcement learning models that analyse past trades decisions and overall outcomes, enabling the bot to adapt and optimise future trading strategies. It allows on-device inference and training using historical data to improve decision-making over time.
