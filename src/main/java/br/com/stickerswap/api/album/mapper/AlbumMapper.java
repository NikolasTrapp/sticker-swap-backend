package br.com.stickerswap.api.album.mapper;

import br.com.stickerswap.api.album.dto.AlbumResponse;
import br.com.stickerswap.api.album.dto.CreateAlbumRequest;
import br.com.stickerswap.api.album.dto.CreateStickerRequest;
import br.com.stickerswap.api.album.dto.StickerResponse;
import br.com.stickerswap.api.album.dto.UpdateAlbumRequest;
import br.com.stickerswap.api.album.dto.UpdateStickerRequest;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AlbumMapper {

    AlbumResponse toAlbumResponse(Album album);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Album toAlbum(CreateAlbumRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateAlbum(UpdateAlbumRequest request, @MappingTarget Album album);

    StickerResponse toStickerResponse(Sticker sticker);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "albumId", source = "albumId")
    Sticker toSticker(CreateStickerRequest request, UUID albumId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "albumId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    void updateSticker(UpdateStickerRequest request, @MappingTarget Sticker sticker);
}
