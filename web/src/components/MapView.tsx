import React, { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { Complaint } from "../data/models";

// Fix Leaflet's default marker icon assets path resolution in Vite bundles
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
  iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png"
});

interface MapViewProps {
  complaints: Complaint[];
  isHeatmapMode: boolean;
  onLocationSelected?: (lat: number, lng: number, address: string) => void;
  onPinClicked?: (complaint: Complaint) => void;
  height?: string;
}

export const MapView: React.FC<MapViewProps> = ({
  complaints,
  isHeatmapMode,
  onLocationSelected,
  onPinClicked,
  height = "300px"
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markersGroupRef = useRef<L.LayerGroup | null>(null);
  const circlesGroupRef = useRef<L.LayerGroup | null>(null);
  const userMarkerRef = useRef<L.Marker | null>(null);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    // Default center at Richmond Circle, Bengaluru
    const map = L.map(mapContainerRef.current, {
      center: [12.97159, 77.59456],
      zoom: 14,
      zoomControl: true,
      doubleClickZoom: false // disable to allow double click pinning
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    }).addTo(map);

    markersGroupRef.current = L.layerGroup().addTo(map);
    circlesGroupRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;

    // Handle Map Clicks for Pinning
    if (onLocationSelected) {
      map.on("click", (e: L.LeafletMouseEvent) => {
        const { lat, lng } = e.latlng;
        
        // Remove previous user pin
        if (userMarkerRef.current) {
          map.removeLayer(userMarkerRef.current);
        }

        // Add new blue user marker
        const userIcon = new L.Icon({
          iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png",
          shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41]
        });

        userMarkerRef.current = L.marker([lat, lng], { icon: userIcon }).addTo(map);
        
        const address = `Pin location near Lat: ${lat.toFixed(5)}, Lng: ${lng.toFixed(5)}`;
        onLocationSelected(lat, lng, address);
      });
    }

    // Fix map layout on resize
    const resizeObserver = new ResizeObserver(() => {
      map.invalidateSize();
    });
    resizeObserver.observe(mapContainerRef.current);

    return () => {
      resizeObserver.disconnect();
      map.remove();
      mapRef.current = null;
    };
  }, [onLocationSelected]);

  // Update Markers & Heatmap Circles
  useEffect(() => {
    const map = mapRef.current;
    const markersGroup = markersGroupRef.current;
    const circlesGroup = circlesGroupRef.current;

    if (!map || !markersGroup || !circlesGroup) return;

    // Clear previous items
    markersGroup.clearLayers();
    circlesGroup.clearLayers();

    // Renders Circles
    if (isHeatmapMode) {
      complaints.forEach(c => {
        let color = "#00E676"; // Low
        if (c.priority === "High") color = "#FF3D00";
        else if (c.priority === "Medium") color = "#FFC107";

        L.circle([c.latitude, c.longitude], {
          color: "transparent",
          fillColor: color,
          fillOpacity: 0.45,
          radius: 200 // meters
        }).addTo(circlesGroup);
      });
    }

    // Render Markers
    complaints.forEach(c => {
      // Choose icon color based on Category
      let colorName = "red";
      if (c.category.includes("Pothole")) colorName = "orange";
      else if (c.category.includes("Water")) colorName = "blue";
      else if (c.category.includes("Drainage")) colorName = "green";
      else if (c.category.includes("Streetlight")) colorName = "yellow";
      else if (c.category.includes("Traffic")) colorName = "violet";

      const customIcon = new L.Icon({
        iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-${colorName}.png`,
        shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      });

      const marker = L.marker([c.latitude, c.longitude], { icon: customIcon })
        .addTo(markersGroup)
        .bindPopup(`<b>${c.title}</b><br/>${c.category} &bull; ${c.status}`);

      marker.on("click", () => {
        if (onPinClicked) {
          onPinClicked(c);
        }
      });
    });
  }, [complaints, isHeatmapMode, onPinClicked]);

  return (
    <div 
      ref={mapContainerRef} 
      style={{ 
        height, 
        width: "100%", 
        borderRadius: "12px", 
        border: "1px solid var(--border-color)",
        zIndex: 1 
      }} 
    />
  );
};
