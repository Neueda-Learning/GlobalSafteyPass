package com.travelassistant.service;

import com.fasterxml.jackson.databind.*;
import com.travelassistant.dto.ApiDtos.*;
import com.travelassistant.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AtmMapService {
    private final WebClient geocoding;private final WebClient places;
    private final Map<String,MapLocationResponse> geocodeCache=new ConcurrentHashMap<>();
    private final Map<String,AtmSearchResponse> atmCache=new ConcurrentHashMap<>();
    public AtmMapService(@Qualifier("mapGeocodingWebClient")WebClient geocoding,
            @Qualifier("mapPlacesWebClient")WebClient places){this.geocoding=geocoding;this.places=places;}

    public MapLocationResponse geocode(String query){
        String normalized=query.trim();if(normalized.length()<2)throw new IllegalArgumentException("Enter a location.");
        return geocodeCache.computeIfAbsent(normalized.toLowerCase(Locale.ROOT),key->{
            JsonNode body;
            try{
                body=geocoding.get().uri(uri->uri.path("/search").queryParam("q",normalized)
                        .queryParam("format","jsonv2").queryParam("limit",1).queryParam("addressdetails",1).build())
                        .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(JsonNode.class).block();
            }catch(RuntimeException ex){
                throw new IllegalArgumentException("Location search is temporarily unavailable. Please try again.");
            }
            if(body==null||!body.isArray()||body.isEmpty())throw new ResourceNotFoundException("Location not found.");
            JsonNode first=body.get(0);
            return new MapLocationResponse(normalized,first.path("display_name").asText(normalized),
                    first.path("lat").asDouble(),first.path("lon").asDouble());
        });
    }

    public AtmSearchResponse nearby(double latitude,double longitude,int requestedRadius){
        int radius=Math.max(500,Math.min(5000,requestedRadius));
        String cacheKey=String.format(Locale.ROOT,"%.4f:%.4f:%d",latitude,longitude,radius);
        return atmCache.computeIfAbsent(cacheKey,key->loadNearby(latitude,longitude,radius));
    }
    private AtmSearchResponse loadNearby(double latitude,double longitude,int radius){
        try{
            AtmSearchResponse overpass=loadFromOverpass(latitude,longitude,radius);
            if(!overpass.atms().isEmpty())return overpass;
        }catch(RuntimeException ignored){
            // Public Overpass nodes can be temporarily busy. A bounded Nominatim
            // query keeps this user-initiated recovery flow usable.
        }
        try{return loadFromNominatim(latitude,longitude,radius);}
        catch(RuntimeException ignored){
            return new AtmSearchResponse(latitude,longitude,radius,List.of(),
                    "OpenStreetMap temporarily unavailable");
        }
    }
    private AtmSearchResponse loadFromOverpass(double latitude,double longitude,int radius){
        String query="[out:json][timeout:15];(node[\"amenity\"=\"atm\"](around:"+radius+","+latitude+","+longitude+");"
                +"way[\"amenity\"=\"atm\"](around:"+radius+","+latitude+","+longitude+");"
                +"relation[\"amenity\"=\"atm\"](around:"+radius+","+latitude+","+longitude+"););out center tags;";
        JsonNode body=places.post().uri("/api/interpreter").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("data",query)).retrieve().bodyToMono(JsonNode.class).block();
        List<AtmLocationResponse> found=new ArrayList<>();
        if(body!=null)for(JsonNode element:body.path("elements")){
            JsonNode tags=element.path("tags");
            double lat=element.has("lat")?element.path("lat").asDouble():element.path("center").path("lat").asDouble();
            double lon=element.has("lon")?element.path("lon").asDouble():element.path("center").path("lon").asDouble();
            if(lat==0||lon==0)continue;
            String operator=text(tags,"operator"),brand=text(tags,"brand"),name=text(tags,"name");
            String display=!brand.isBlank()?brand:!operator.isBlank()?operator:!name.isBlank()?name:"ATM";
            String address=address(tags),hours=text(tags,"opening_hours");
            int distance=(int)Math.round(distance(latitude,longitude,lat,lon));
            found.add(new AtmLocationResponse(element.path("type").asText()+"-"+element.path("id").asText(),
                    display,operator,address,hours,lat,lon,distance));
        }
        found.sort(Comparator.comparingInt(AtmLocationResponse::distanceMeters));
        return new AtmSearchResponse(latitude,longitude,radius,found.stream().limit(12).toList(),"OpenStreetMap");
    }
    private AtmSearchResponse loadFromNominatim(double latitude,double longitude,int radius){
        double latDelta=radius/111320d;
        double lonDelta=radius/(111320d*Math.max(0.2,Math.cos(Math.toRadians(latitude))));
        String viewbox=(longitude-lonDelta)+","+(latitude+latDelta)+","
                +(longitude+lonDelta)+","+(latitude-latDelta);
        JsonNode body=geocoding.get().uri(uri->uri.path("/search").queryParam("q","ATM")
                .queryParam("format","jsonv2").queryParam("limit",30).queryParam("addressdetails",1)
                .queryParam("bounded",1).queryParam("viewbox",viewbox).build())
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(JsonNode.class).block();
        List<AtmLocationResponse> found=new ArrayList<>();
        if(body!=null)for(JsonNode place:body){
            if(!"amenity".equals(place.path("category").asText())
                    ||!"atm".equals(place.path("type").asText()))continue;
            double lat=place.path("lat").asDouble(),lon=place.path("lon").asDouble();
            int meters=(int)Math.round(distance(latitude,longitude,lat,lon));
            if(meters>radius)continue;
            String name=place.path("name").asText("");
            if(name.isBlank())name="ATM";
            found.add(new AtmLocationResponse(place.path("osm_type").asText()+"-"+place.path("osm_id").asText(),
                    name,name,nominatimAddress(place), "",lat,lon,meters));
        }
        found.sort(Comparator.comparingInt(AtmLocationResponse::distanceMeters));
        return new AtmSearchResponse(latitude,longitude,radius,found.stream().limit(12).toList(),
                "OpenStreetMap");
    }
    private String nominatimAddress(JsonNode place){
        JsonNode address=place.path("address");
        String number=address.path("house_number").asText("");
        String road=address.path("road").asText("");
        String city=address.path("city").asText(address.path("town").asText(""));
        List<String> parts=new ArrayList<>();
        if(!road.isBlank())parts.add((number+" "+road).trim());
        if(!city.isBlank())parts.add(city);
        if(!parts.isEmpty())return String.join(", ",parts);
        String display=place.path("display_name").asText("");
        int comma=display.indexOf(',');
        return comma>=0?display.substring(comma+1).trim():display;
    }
    private String address(JsonNode tags){
        List<String> parts=new ArrayList<>();
        String number=text(tags,"addr:housenumber"),street=text(tags,"addr:street"),city=text(tags,"addr:city");
        if(!street.isBlank())parts.add((number+" "+street).trim());if(!city.isBlank())parts.add(city);
        return parts.isEmpty()?"Address not listed":String.join(", ",parts);
    }
    private String text(JsonNode node,String key){return node.path(key).asText("");}
    private double distance(double lat1,double lon1,double lat2,double lon2){
        double r=6371000,p1=Math.toRadians(lat1),p2=Math.toRadians(lat2);
        double dp=Math.toRadians(lat2-lat1),dl=Math.toRadians(lon2-lon1);
        double a=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 2*r*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }
}
