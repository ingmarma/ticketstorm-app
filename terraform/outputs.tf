output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.ticketstorm.id
}

output "public_ip" {
  description = "Public IP (Elastic IP)"
  value       = aws_eip.ticketstorm.public_ip
}

output "ssh_command" {
  description = "SSH command to connect"
  value       = "ssh -i ${path.module}/ticketstorm-key.pem ubuntu@${aws_eip.ticketstorm.public_ip}"
}

output "app_url" {
  description = "Application URL"
  value       = "http://${aws_eip.ticketstorm.public_ip}"
}

output "grafana_url" {
  description = "Grafana dashboard URL"
  value       = "http://${aws_eip.ticketstorm.public_ip}:3001"
}

output "ssh_private_key_path" {
  description = "Path to SSH private key"
  value       = local_file.ssh_private_key.filename
}
