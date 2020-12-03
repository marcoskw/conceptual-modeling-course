package com.mkwinformatica.cursomc.services;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mkwinformatica.cursomc.domain.Cidade;
import com.mkwinformatica.cursomc.domain.Cliente;
import com.mkwinformatica.cursomc.domain.Endereco;
import com.mkwinformatica.cursomc.domain.enums.Perfil;
import com.mkwinformatica.cursomc.domain.enums.TipoCliente;
import com.mkwinformatica.cursomc.dto.ClienteDTO;
import com.mkwinformatica.cursomc.dto.ClienteNewDTO;
import com.mkwinformatica.cursomc.repositories.ClienteRepository;
import com.mkwinformatica.cursomc.repositories.EnderecoRepository;
import com.mkwinformatica.cursomc.security.UserSS;
import com.mkwinformatica.cursomc.services.exceptions.AuthorizationException;
import com.mkwinformatica.cursomc.services.exceptions.DataIntegrityException;
import com.mkwinformatica.cursomc.services.exceptions.ObjectNotFoundException;

@Service

public class ClienteService {

	@Autowired
	private ClienteRepository repo;

	@Autowired
	private EnderecoRepository enderecoRepository;
	
	@Autowired
	private BCryptPasswordEncoder pe;
	
	@Autowired
	private S3Service s3Service;

	public Cliente find(Integer id) {
		
		UserSS user = UserService.authenticated();
		
		if (user == null || !user.hasRole(Perfil.ADMIN) && !id.equals(user.getId())) {
			throw new AuthorizationException("Acesso Negado!");
		}
		
		Optional<Cliente> obj = repo.findById(id);
		return obj.orElseThrow(() -> new ObjectNotFoundException(
				"Objeto não encontrado! Id: " + id + ", Tipo: " + Cliente.class.getName()));
	}

	@Transactional
	public Cliente insert(Cliente obj) {
		obj.setId(null);
		obj = repo.save(obj);
		enderecoRepository.saveAll(obj.getEnderecos());
		return obj;
	}

	public Cliente update(Cliente obj) {
		Cliente newObj = find(obj.getId());
		updateData(newObj, obj);
		return repo.save(newObj);
	}

	public void delete(Integer id) {
		find(id);
		try {
			repo.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DataIntegrityException("Não é possível excluir porque há pedidos relacionadas");
		}
	}

	public List<Cliente> findAll() {
		return repo.findAll();
	}

	public Page<Cliente> findPage(Integer page, Integer linesPerPage, String orderBy, String direction) {
		PageRequest pageRequest = PageRequest.of(page, linesPerPage, Direction.valueOf(direction), orderBy);
		return repo.findAll(pageRequest);
	}

	public Cliente fromDTO(ClienteDTO objDto) {
		return new Cliente(objDto.getId(), objDto.getNome(), objDto.getEmail(), null, null, null);
	}

	public Cliente fromDTO(ClienteNewDTO objDto) {
		Cliente cliente = new Cliente(null, 
							      objDto.getNome(), 
							      objDto.getEmail(), 
							      objDto.getCpfOuCnpj(),
							      TipoCliente.toEnum(objDto.getTipo()),
							      pe.encode(objDto.getSenha()));
		Cidade cidade = new Cidade(objDto.getCidadeId(), null, null);
		
		Endereco endereco = new Endereco(null, 
									objDto.getLogradouro(), 
									objDto.getNumero(), 
									objDto.getComplemento(),
									objDto.getBairro(), 
									objDto.getCep(), cliente, cidade);
		cliente.getEnderecos().add(endereco);
		cliente.getTelefones().add(objDto.getTelefone1());
		if (objDto.getTelefone2() != null) {
			cliente.getTelefones().add(objDto.getTelefone2());
		}
		if (objDto.getTelefone3() != null) {
			cliente.getTelefones().add(objDto.getTelefone3());
		}
		return cliente;
	}

	private void updateData(Cliente newObj, Cliente obj) {
		newObj.setNome(obj.getNome());
		newObj.setEmail(obj.getEmail());
	}
	
	public URI uploadProfilePicture(MultipartFile multipartFile) {
		UserSS user = UserService.authenticated();
		if (user == null) {
			throw new AuthorizationException("Acesso negado");
		}
		
		URI uri = s3Service.uploadFile(multipartFile);
		
		Cliente cli = repo.findByEmail(user.getUsername());
		cli.setImageUrl(uri.toString());
		repo.save(cli);
		
		return uri;
	}

}