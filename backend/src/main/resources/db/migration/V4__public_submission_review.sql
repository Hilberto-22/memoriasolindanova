alter table contributions alter column collection_slug drop not null;

update portal_content
set community_description = 'Envie textos ou imagens sobre a história de Olinda Nova do Maranhão para avaliação do administrador.'
where id = 1 and community_description = 'Contribua com fotografias, documentos, relatos ou objetos que façam parte da história de Olinda Nova do Maranhão.';
