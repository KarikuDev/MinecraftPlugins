(; program to build houses and cities

  ; see http://www.minecraftwiki.net/wiki/Data_values#Block_IDs for the block names
  (val house-blocks (list 1 4 5 7 14 15 16 17 20 21 22 24 35 41 42 43 45 47
                          48 49 56 57 73 74 87 89 98 112 121 23 124 125 129 133))
  (val nr-house-blocks (.size house-blocks))
  (def random-house-block [] (material (.apply house-blocks (random-int 0 nr-house-blocks))))

  ; build a pyramid!
  ; Cube -> Material -> Cube
  (defrec pyramid [c m]
    (cube:set-walls c m)
    (let (closed (fn [c] (or (<= (- (.maxX c) (.minX c)) 1) (<= (- (.maxZ c) (.minZ c)) 1))))
      (unless (closed c) (pyramid (.shiftY (.shrinkXZ c 1) 1) m))
    )
  )

  ; build a single house
  ; Location -> Int -> Int -> Int -> Material -> Material -> Material -> Cube
  (def build-house [start-point h w d floor-m walls-m roof-m]
    (let* ((c (.growUp (.expandZ (.expandX (cube:mk start-point start-point) w) d) h))
           (build-ceiling (fn (c m) (pyramid (.expandXZ (.ceiling c) 1) m) c)))
      (begin
        (build-ceiling (cube:set-walls (cube:set-floor (cube:set-all c "air") floor-m) walls-m) roof-m)
        c
      )
    )
  )

  ; build a row of houses
  ; Location -> Int -> (Location -> Cube) -> Cube
  (defrec build-house-row [at nr-houses house-builder-f]
    (unless (eq? nr-houses 0)
      (let (c (house-builder-f at))
        ; TODO: 20 isnt right here. the houses could be bigger than 20 wide...
        (build-house-row
          (loc
            (+ (* 2 (.width c)) (.getX at))
            (.getY at)
            (.getZ at)
          )
          (- nr-houses 1)
          house-builder-f
        )
      )
    )
  )

  (def random-building [center-point
                        min-h max-h
                        min-w max-w
                        min-d max-d]
    (build-house
      center-point
      (random-int min-h max-h)
      (random-int min-w max-w)
      (random-int max-w max-d)
      (random-house-block)
      (random-house-block)
      (random-house-block)
    )
  )

  ; builds a skyscraper
  ; Location -> Cube
  (def build-skyscraper [l] (build-house l 50 8 10 "stone" "obsidian" "diamond_block"))
  (def build-random-skyscraper [l] (random-building l
      20 100 ; min-h max-h
       4  10 ; min-w max-w
       4  10 ; min-d max-d
  ))

  ; builds a house - {5 -> wood plank, 17 -> wood, 20 -> glass}
  ; Location -> Cube
  (def build-normal-house [l] (build-house l 4 3 3 "5" "17" "20"))

  ; builds a row of skyscrapers. not really a full city, yet.
  ; -> Cube
  (def city    [] (build-house-row (XYZ) 10 build-random-skyscraper))

  ; builds a row of little houses. could be considered a village.
  ; -> Cube
  (def village [] (build-house-row (XYZ)  6 build-normal-house))

  ; build a house, and then change the wall its walls every second for 100 seconds.
  (def living-house []
    (let (c (normal-house (XYZ)))
      (spawn 100 1 (fn (n) (cube:set-walls c (if (even n) "gold_block" "gold_ore"))))
    )
  )
)